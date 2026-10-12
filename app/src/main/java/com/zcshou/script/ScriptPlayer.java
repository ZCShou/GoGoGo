package com.zcshou.script;

import java.util.List;

/**
 * 脚本播放引擎：按时间推进，在路点之间做经纬度插值。
 *
 * <p>播放本身不关心线程，由 {@link com.zcshou.service.ServiceGo} 的定位线程周期性调用
 * {@link #onTick()} 推进即可。每个 {@link ScriptWaypoint} 自带移动状态（走 / 跑 / 骑）和速度，
 * 因此“同一段脚本里既能走路又能骑车”是靠分段速度实现的：切线方向不变，速度随当前段的状态变化。</p>
 *
 * <p>start / stop 可能在主线程调用，而 onTick 在定位线程调用，所以这几个入口都做成同步方法，
 * 回调也统一由引擎自己加锁，避免“停止播放”和“推进位置”交叉执行。</p>
 *
 * <p>支持暂停 / 继续：暂停期间时间轴整体冻结（onTick 直接空转），
 * 继续时把暂停的时长从起始时刻里补回去，因此恢复后从暂停处原地接着走，
 * 既不会跳过路段，也不会瞬移回别处。</p>
 */
public class ScriptPlayer {
    /** 地球平均半径（米） */
    public static final double EARTH_RADIUS = 6371008.8D;

    /** 上一次位置与本次位置小于该距离时，认为没有移动（用于避免重复注入相同坐标） */
    private static final double MOVE_EPSILON_METERS = 0.05D;
    /** 计算下一位置时允许的最小推进距离，避免除零 */
    private static final double MIN_STEP_METERS = 0.01D;
    /** 速度兜底值（米/秒），避免除零 */
    private static final double MIN_SPEED = 0.1D;

    private ScriptRoute mRoute;
    private boolean mPlaying;
    private boolean mEndReached;
    /** 是否处于暂停中：暂停期间 onTick 空转，位置停在原地 */
    private boolean mPaused;
    /** 进入暂停的时刻（毫秒），继续播放时把这段时长从时间轴里补回去 */
    private long mPausedRealtime;

    /** 开始播放的时刻（毫秒，用来把等待时间和移动时间统一成一条时间轴） */
    private long mStartRealtime;
    /**
     * 已经“消费掉”的时间轴长度（秒）：走过路段、到站停留都计入。
     *
     * <p>每个 tick 的有效时间 = (now - mStartRealtime) / 1000 - mConsumedSeconds，
     * 也就是“从当前路段起点算起经过的秒数”。以前没有这个扣减，全局累计时间会被
     * 从当前段重新扣一遍，导致段落被成片跳过、脚本提前跑完。</p>
     */
    private double mConsumedSeconds;
    /** 当前正在走的路段：从 mIndex 到 mIndex + 1 */
    private int mIndex;
    /** 是否循环 */
    private boolean mLoop;
    /** 是否已经产生过有效位置（用于首次强制回调一次） */
    private boolean mHasPosition;

    /**
     * 播放过程中手动切换的移动状态（走 / 跑 / 骑）。
     *
     * <p>非 null 时它会覆盖后面路点自带的状态与速度，直到再次切换或脚本结束，
     * 这样“边走边切”不需要停下来重新开始。</p>
     */
    private ScriptWaypoint.Mode mOverrideMode;
    /** 与 {@link #mOverrideMode} 配套的速度（米/秒），<= 0 表示没有手动切换 */
    private double mOverrideSpeed;
    /**
     * 上一次 {@link #onTick()} 结束时，当前路段上已经消耗的时间（秒）。
     *
     * <p>切换速度时用它换算出“已经走过的距离”，再按新速度重算时间轴，
     * 位置就不会跳变。</p>
     */
    private double mElapsedInSegment;

    private double mLng;
    private double mLat;
    private ScriptWaypoint.Mode mMode = ScriptWaypoint.Mode.WALK;
    private double mSpeed;
    private double mBearing;
    private double mAltitude;

    private Handler mHandler;

    /** 播放过程中的回调，实现方需要保证线程安全（默认在 ServiceGo 的定位线程里回调） */
    public interface Handler {
        /** 开始播放，参数为第一个路点的位置 */
        void onScriptStart(ScriptRoute route, double lng, double lat, double alt);

        /** 位置推进（每一步都保证确实移动了） */
        void onScriptPosition(double lng, double lat, double alt, ScriptWaypoint.Mode mode, double speed, double bearing);

        /** 进入某一段（该段起点的状态），可以用来提示“开始骑行” */
        void onScriptSegment(ScriptRoute route, int index, ScriptWaypoint.Mode mode, double speed);

        /** 单次模式跑到终点 */
        void onScriptFinish(ScriptRoute route);

        /** 异常停止（脚本无效等） */
        void onScriptError(String message);
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public synchronized boolean isPlaying() {
        return mPlaying && mRoute != null;
    }

    /**
     * 当前路段的移动状态（走 / 跑 / 骑）。
     *
     * <p>这是本类唯一对外暴露的“当前状态”读数，被 ServiceGo 的界面状态查询使用；
     * 位置 / 速度 / 方向这些内部状态不提供 getter——它们只在本类内部参与插值计算，
     * 暴露出去只会让人误以为可以外部改写，徒增维护成本。</p>
     */
    public ScriptWaypoint.Mode getMode() {
        return mMode;
    }

    /**
     * 开始播放。
     *
     * @param route       已经被解析好的路线（路点至少一个）
     * @param fromLng     模拟位置当前所在经度，用于把脚本对齐到最近的轨迹点
     * @param fromLat     模拟位置当前所在纬度
     */
    public synchronized void start(ScriptRoute route, double fromLng, double fromLat) {
        if (route == null || route.points == null || route.points.isEmpty()) {
            notifyError("脚本没有可用的路点");
            return;
        }

        mRoute = route;
        mLoop = route.isLoop();
        mEndReached = false;
        mHasPosition = false;
        mPaused = false;
        mOverrideMode = null;
        mOverrideSpeed = 0;
        mElapsedInSegment = 0;
        mConsumedSeconds = 0;

        List<ScriptWaypoint> points = route.points;

        // 只有一个点：原地保持该状态（不会移动，但速度 / 方向按该点设置）
        if (points.size() == 1) {
            mIndex = 0;
            mLng = points.get(0).lng;
            mLat = points.get(0).lat;
            mAltitude = points.get(0).alt;
            mMode = points.get(0).mode;
            mSpeed = points.get(0).speed;
            mBearing = 0;
            mStartRealtime = System.currentTimeMillis();
            mPlaying = true;

            if (mHandler != null) {
                mHandler.onScriptStart(route, mLng, mLat, mAltitude);
                mHandler.onScriptSegment(route, 0, mMode, mSpeed);
            }
            return;
        }

        Anchor anchor = findAnchor(points, fromLng, fromLat);

        // 站在（或越过）单次路线的终点时按“开始”：接入点会落在最后一段的尽头，
        // 第一个 tick 就直接判“已到终点”，表现为“点开始后一动不动”。
        // 这里改成从头重新走，保证任何入口按下“开始”都一定有位移。
        if (!mLoop && anchor.index >= points.size() - 2) {
            ScriptWaypoint lastFrom = points.get(anchor.index);
            ScriptWaypoint lastTo = points.get(anchor.index + 1);
            double lastTravel = travelTime(ScriptParser.distance(lastFrom, lastTo),
                    speedOf(lastFrom), speedOf(lastTo));
            if (anchor.offsetSeconds >= lastTravel - 0.05D) {
                anchor.index = 0;
                anchor.offsetSeconds = 0;
            }
        }

        mIndex = anchor.index;

        ScriptWaypoint current = points.get(mIndex);
        mLng = current.lng;
        mLat = current.lat;
        mAltitude = current.alt;
        mMode = current.mode;
        mSpeed = current.speed;
        mBearing = ScriptParser.bearing(current, points.get(mIndex + 1));

        // 接入点之前已经走过的路程折进时间轴：t=0 时位置正好落在接入点上，不会瞬移
        mStartRealtime = System.currentTimeMillis() - (long) Math.round(anchor.offsetSeconds * 1000.0);
        mPlaying = true;

        if (mHandler != null) {
            mHandler.onScriptStart(route, mLng, mLat, mAltitude);
            mHandler.onScriptSegment(route, mIndex, mMode, mSpeed);
        }

        // 立即推进一次，避免第一次 tick 之前位置没变化
        onTick();
    }

    /** 停止播放（保留当前位置，不再推进） */
    public synchronized void stop() {
        mPlaying = false;
        mEndReached = false;
        mPaused = false;
        mRoute = null;
        mOverrideMode = null;
        mOverrideSpeed = 0;
        mElapsedInSegment = 0;
        mConsumedSeconds = 0;
    }

    /**
     * 暂停播放：位置停在原地，时间轴冻结。
     *
     * <p>暂停不等于停止：路线、进度、手动切换过的状态全部保留，
     * 只是不再按时间推进；恢复时从暂停的位置原地接着走。</p>
     *
     * <p>没在播放或已到终点时<b>不会</b>进入暂停——调用方（ServiceGo）靠返回值区分
     * “真暂停了”和“这一拍刚好跑完”，否则 FINISHED 状态会被误改成“暂停中”。</p>
     *
     * @return 是否进入暂停
     */
    public synchronized boolean pause() {
        if (!mPlaying || mRoute == null || mEndReached || mPaused) {
            return false;
        }

        mPaused = true;
        mPausedRealtime = System.currentTimeMillis();
        return true;
    }

    /**
     * 继续播放暂停中的脚本。
     *
     * <p>把暂停的时长加回起始时刻，等价于“这段时间里时钟没走”，
     * 因此恢复后 elapsed 从暂停处继续，不会跳过任何路段。</p>
     *
     * @return 是否恢复成功（没在暂停时返回 false）
     */
    public synchronized boolean resume() {
        if (!mPlaying || mRoute == null || mEndReached || !mPaused) {
            return false;
        }

        mStartRealtime += System.currentTimeMillis() - mPausedRealtime;
        mPaused = false;

        // 单点脚本位置永远不变：恢复后坐标不动，位置回调不会再触发（moved=false），
        // 注入速度会一直停在暂停时写下的 0，这里按该点的状态强制补发一次
        List<ScriptWaypoint> points = mRoute.points;
        if (points != null && points.size() == 1) {
            ScriptWaypoint only = points.get(0);
            updatePosition(only.lng, only.lat, only.alt, liveMode(only), liveSpeed(only), mBearing, true);
        }

        return true;
    }

    /** 是否处于暂停中（与 {@link #isPlaying()} 互补：暂停时仍在播放，只是不推进） */
    public synchronized boolean isPaused() {
        return mPaused;
    }

    /**
     * 播放过程中手动切换移动状态（走 / 跑 / 骑）。
     *
     * <p>切换时先按当前已走过的距离重新对齐时间轴，再让剩下的路以新速度走完，
     * 因此位置不会跳变。切换会一直生效到再次切换或脚本结束。</p>
     *
     * @param mode  新的移动状态
     * @param speed 新状态对应的速度（米/秒），必须大于 0
     * @return 是否切换成功（没有在播放、参数非法时返回 false）
     */
    public synchronized boolean setLiveMode(ScriptWaypoint.Mode mode, double speed) {
        if (!mPlaying || mRoute == null || mEndReached || mode == null || speed <= 0) {
            return false;
        }

        List<ScriptWaypoint> points = mRoute.points;
        if (points == null || points.isEmpty()) {
            return false;
        }

        // 单点脚本：没有路段可走，只把状态和速度换掉
        if (points.size() < 2) {
            mOverrideMode = mode;
            mOverrideSpeed = speed;
            return true;
        }

        // 必须在改 mOverrideSpeed 之前算，此时用的还是“切换前”的速度模型
        double covered = coveredDistanceInSegment();
        double segment = segmentLength();

        mOverrideMode = mode;
        mOverrideSpeed = speed;

        // 新模型下 v0 = v1 = 新速度，所以“已走 covered 米”等价于新时间轴上的 covered / speed 秒。
        // 时间轴 = 原始时钟 - 已消费时间，所以要把 mConsumedSeconds 一起加回去。
        // 暂停中改状态时，时钟按“停在暂停那一刻”算：暂停的时间不参与推进，
        // 这样继续播放时 elapsed 正好从切换时的进度接着走
        double elapsed = Math.min(segment, Math.max(0, covered)) / speed;
        long now = mPaused ? mPausedRealtime : System.currentTimeMillis();
        mStartRealtime = now - (long) Math.round((elapsed + mConsumedSeconds) * 1000.0);
        mElapsedInSegment = elapsed;

        return true;
    }

    /** 当前播放中手动切换的状态，没有切换过时返回 null */
    public synchronized ScriptWaypoint.Mode getLiveMode() {
        return mOverrideMode;
    }

    /**
     * 周期调用：按“开始播放到现在”的时间推进位置。
     *
     * <p>用时间轴而不是累加距离，可以让每段的移动速度严格等于该段状态的速度
     * （包括停留时间，也不需要额外计时器）。</p>
     */
    public synchronized boolean onTick() {
        if (!mPlaying || mRoute == null || mEndReached || mPaused) {
            return false;
        }

        List<ScriptWaypoint> points = mRoute.points;
        if (points == null || points.isEmpty()) {
            return false;
        }

        // 单点脚本：位置不动，只有速度 / 方向需要维持
        if (points.size() == 1) {
            ScriptWaypoint only = points.get(0);
            return updatePosition(only.lng, only.lat, only.alt, liveMode(only),
                    liveSpeed(only), mBearing, false);
        }

        // 有效时间 = 从接入点开始的全局时钟 - 已消费的时间，得到“当前路段上已过的秒数”
        double elapsed = (System.currentTimeMillis() - mStartRealtime) / 1000.0 - mConsumedSeconds;
        if (elapsed < 0) {
            elapsed = 0;
        }

        double remainingTime = elapsed;
        int guard = 0;

        // 每轮循环里的 remainingTime 都表示“从当前路段起点算起经过的秒数”，
        // 每跨过一段就把该段消耗的时间记入 mConsumedSeconds，保证跨 tick 时仍然成立。
        while (guard++ < 100000) {
            if (remainingTime < 0) {
                remainingTime = 0;
            }

            int next = mIndex + 1;

            if (next >= points.size()) {
                // 已经到了最后一个点
                if (!mLoop) {
                    ScriptWaypoint last = points.get(points.size() - 1);
                    mEndReached = true;
                    mElapsedInSegment = remainingTime;
                    // 终点的停留已经在最后一段的预算里消耗完，这里只报“已停下”
                    updatePosition(last.lng, last.lat, last.alt, liveMode(last), 0, mBearing, true);

                    if (mHandler != null) {
                        mHandler.onScriptFinish(mRoute);
                    }
                    return true;
                }

                // 循环模式：补上“最后一点回到第一点”的闭合段
                ScriptWaypoint last = points.get(mIndex);
                ScriptWaypoint first = points.get(0);
                double closing = ScriptParser.distance(last, first);
                double closingV0 = liveSpeed(last);
                double closingV1 = liveSpeed(first);

                if (closing < MIN_STEP_METERS) {
                    mIndex = 0;
                    mElapsedInSegment = remainingTime;
                    mBearing = points.size() > 1 ? ScriptParser.bearing(first, points.get(1)) : 0;
                    if (mHandler != null) {
                        mHandler.onScriptSegment(mRoute, 0, liveMode(first), closingV1);
                    }
                    continue;
                }

                double closingTravel = travelTime(closing, closingV0, closingV1);
                // 走完闭合段就回到起点，所以这里消耗的是“起点”的停留时间
                // （最后一点自己的停留，早在进入这一点那一段里就已经消耗过了）
                double closingWait = waitOf(first);
                if (remainingTime < closingTravel + closingWait) {
                    double moving = Math.max(0, Math.min(closingTravel, remainingTime));
                    double distance = distanceAt(closingV0, closingV1, moving, closingTravel, closing);
                    double speed = remainingTime < closingTravel ? speedAt(closingV0, closingV1, moving, closingTravel) : 0;
                    double bearing = ScriptParser.bearing(last, first);
                    double[] position = destination(last.lat, last.lng, bearing, distance);
                    mElapsedInSegment = remainingTime;
                    return updatePosition(position[0], position[1], last.alt, liveMode(last), speed, bearing, true);
                }

                remainingTime -= closingTravel + closingWait;
                mConsumedSeconds += closingTravel + closingWait;
                mIndex = 0;
                mElapsedInSegment = remainingTime;
                mBearing = points.size() > 1 ? ScriptParser.bearing(first, points.get(1)) : 0;
                if (mHandler != null) {
                    mHandler.onScriptSegment(mRoute, 0, liveMode(first), closingV1);
                }
                continue;
            }

            ScriptWaypoint from = points.get(mIndex);
            ScriptWaypoint to = points.get(next);
            double segment = ScriptParser.distance(from, to);

            // 位置重合的相邻点：直接跨过去
            if (segment < MIN_STEP_METERS) {
                mIndex = next;
                mElapsedInSegment = remainingTime;
                mBearing = next + 1 < points.size()
                        ? ScriptParser.bearing(to, points.get(next + 1))
                        : ScriptParser.bearing(to, mLoop ? points.get(0) : to);
                if (mHandler != null) {
                    mHandler.onScriptSegment(mRoute, mIndex, liveMode(to), liveSpeed(to));
                }
                continue;
            }

            // 段内速度从起点的状态速度线性过渡到终点的状态速度（走 / 跑 / 骑之间不再瞬变）
            double v0 = liveSpeed(from);
            double v1 = liveSpeed(to);
            double travel = travelTime(segment, v0, v1);
            double dwell = waitOf(to);

            if (remainingTime < travel + dwell) {
                // 先在 travel 秒内走完这一段，然后停在终点消耗停留时间（速度报 0，更像真人）
                boolean moving = remainingTime < travel;
                double movingSeconds = moving ? remainingTime : travel;
                double distance = distanceAt(v0, v1, movingSeconds, travel, segment);
                double speed = moving ? speedAt(v0, v1, movingSeconds, travel) : 0;
                double bearing = ScriptParser.bearing(from, to);
                double[] position = destination(from.lat, from.lng, bearing, distance);
                mElapsedInSegment = remainingTime;
                return updatePosition(position[0], position[1], from.alt, liveMode(from), speed, bearing, true);
            }

            // 这一段（含到站停留）已经走完，进入下一站
            remainingTime -= travel + dwell;
            mConsumedSeconds += travel + dwell;
            mIndex = next;
            mElapsedInSegment = remainingTime;
            mBearing = next + 1 < points.size()
                    ? ScriptParser.bearing(to, points.get(next + 1))
                    : ScriptParser.bearing(to, mLoop ? points.get(0) : to);

            if (mHandler != null) {
                mHandler.onScriptSegment(mRoute, mIndex, liveMode(to), liveSpeed(to));
            }
        }

        return false;
    }

    /**
     * 更新位置，位置确实变化时回调。
     *
     * @param force 强制回调（例如到达终点时，即使坐标没变也要刷新一次速度 / 方向）
     * @return 是否产生了回调
     */
    private boolean updatePosition(double lng, double lat, double alt, ScriptWaypoint.Mode mode,
                                   double speed, double bearing, boolean force) {
        boolean moved = force || !mHasPosition
                || distanceMeters(mLng, mLat, lng, lat) > MOVE_EPSILON_METERS;

        mLng = lng;
        mLat = lat;
        mAltitude = alt > 0 ? alt : mAltitude;
        mMode = mode == null ? ScriptWaypoint.Mode.WALK : mode;
        mSpeed = speed;
        mBearing = (bearing + 360.0) % 360.0;

        if (moved) {
            mHasPosition = true;
            if (mHandler != null) {
                mHandler.onScriptPosition(mLng, mLat, mAltitude, mMode, mSpeed, mBearing);
            }
        }

        return moved;
    }

    private void notifyError(String message) {
        mPlaying = false;
        if (mHandler != null) {
            mHandler.onScriptError(message);
        }
    }

    /** 播放中手动切换过状态时，用切换后的速度；否则用路点自己的速度 */
    private double liveSpeed(ScriptWaypoint point) {
        return mOverrideSpeed > 0 ? mOverrideSpeed : speedOf(point);
    }

    /** 播放中手动切换过状态时，回调里报切换后的状态 */
    private ScriptWaypoint.Mode liveMode(ScriptWaypoint point) {
        if (mOverrideMode != null) {
            return mOverrideMode;
        }
        return point == null || point.mode == null ? ScriptWaypoint.Mode.WALK : point.mode;
    }

    /** 当前路段的长度（米）；循环模式下的闭合段也算在内 */
    private double segmentLength() {
        List<ScriptWaypoint> points = mRoute == null ? null : mRoute.points;
        if (points == null || points.size() < 2) {
            return 0;
        }

        if (mIndex + 1 < points.size()) {
            return ScriptParser.distance(points.get(mIndex), points.get(mIndex + 1));
        }
        return ScriptParser.distance(points.get(mIndex), points.get(0));
    }

    /** 按上一次 tick 留下的时间推算当前路段上已经走过的距离（米） */
    private double coveredDistanceInSegment() {
        List<ScriptWaypoint> points = mRoute == null ? null : mRoute.points;
        if (points == null || points.size() < 2) {
            return 0;
        }

        ScriptWaypoint from = points.get(mIndex);
        ScriptWaypoint to = mIndex + 1 < points.size() ? points.get(mIndex + 1) : points.get(0);

        double segment = ScriptParser.distance(from, to);
        double v0 = liveSpeed(from);
        double v1 = liveSpeed(to);
        double travel = travelTime(segment, v0, v1);
        double moving = Math.max(0.0, Math.min(travel, mElapsedInSegment));

        return distanceAt(v0, v1, moving, travel, segment);
    }

    /** 速度非法时回退到一个安全值，避免除零 */
    private static double speedOf(ScriptWaypoint point) {
        if (point == null) {
            return 1.2D;
        }
        return point.speed > 0 ? point.speed : 1.2D;
    }

    private static int waitOf(ScriptWaypoint point) {
        return point == null ? 0 : Math.max(0, point.waitSeconds);
    }

    /**
     * 走完一段所需的时间。
     *
     * <p>段内速度按“起点速度 → 终点速度”线性变化，平均速度正好是两者的算术平均，
     * 所以耗时 = 2 × 距离 / (v0 + v1)。两个速度相同时退化成原来的“距离 / 速度”，
     * 因此只选一种状态（走 / 跑 / 骑）的脚本，行为与优化前完全一致。</p>
     */
    private static double travelTime(double distance, double v0, double v1) {
        double sum = v0 + v1;
        if (sum <= 0) {
            return distance / Math.max(v0, MIN_SPEED);
        }
        return 2.0 * distance / sum;
    }

    /** 段内已经走了 moving 秒时的瞬时速度（米/秒），用于注入给系统的 speed 字段 */
    private static double speedAt(double v0, double v1, double moving, double travel) {
        if (travel <= 0) {
            return v1;
        }
        double ratio = Math.max(0.0, Math.min(1.0, moving / travel));
        return v0 + (v1 - v0) * ratio;
    }

    /** 段内已经走了 moving 秒时累计走过的距离（米），是 speedAt 的时间积分 */
    private static double distanceAt(double v0, double v1, double moving, double travel, double total) {
        if (travel <= 0) {
            return 0;
        }
        double ratio = Math.max(0.0, Math.min(1.0, moving / travel));
        double distance = (v0 * ratio + (v1 - v0) * ratio * ratio / 2.0) * travel;
        return Math.max(0.0, Math.min(total, distance));
    }

    /** 起点锚点：脚本从轨迹上的哪一段、已经走了多远（换算成时间）开始 */
    private static class Anchor {
        int index;
        /** 在本段上已经走过的移动时间（秒） */
        double offsetSeconds;
    }

    /**
     * 把脚本对齐到离当前位置最近的轨迹点：找到投影最近的线段，并算出已经走过的比例。
     * 这样“人已经在路上”时启动脚本不会出现瞬移。
     */
    private static Anchor findAnchor(List<ScriptWaypoint> points, double lng, double lat) {
        Anchor anchor = new Anchor();
        anchor.index = 0;
        anchor.offsetSeconds = 0;

        int segmentCount = points.size() - 1;
        double best = Double.MAX_VALUE;

        for (int i = 0; i < segmentCount; i++) {
            ScriptWaypoint from = points.get(i);
            ScriptWaypoint to = points.get(i + 1);
            double scale = Math.cos(Math.toRadians(lat));
            double ax = (to.lng - from.lng) * scale;
            double ay = to.lat - from.lat;
            double px = (lng - from.lng) * scale;
            double py = lat - from.lat;

            double lengthSquared = ax * ax + ay * ay;
            double t = 0;
            if (lengthSquared > 0) {
                t = (px * ax + py * ay) / lengthSquared;
                t = Math.max(0.0, Math.min(1.0, t));
            }

            double dx = px - ax * t;
            double dy = py - ay * t;
            double distance = Math.sqrt((dx * 111320.0) * (dx * 111320.0) + (dy * 111320.0) * (dy * 111320.0));

            if (distance < best) {
                best = distance;
                anchor.index = i;
                anchor.offsetSeconds = t * ScriptParser.distance(from, to) / speedOf(from);
            }
        }

        return anchor;
    }

    /**
     * 已知起点、方位角和距离，求目的点（球面公式）。
     *
     * @return {经度, 纬度}
     */
    public static double[] destination(double lat, double lng, double bearingDegrees, double distanceMeters) {
        double angular = distanceMeters / EARTH_RADIUS;
        double bearing = Math.toRadians(bearingDegrees);
        double lat1 = Math.toRadians(lat);
        double lng1 = Math.toRadians(lng);

        double sinLat2 = Math.sin(lat1) * Math.cos(angular) + Math.cos(lat1) * Math.sin(angular) * Math.cos(bearing);
        double lat2 = Math.asin(Math.max(-1.0, Math.min(1.0, sinLat2)));

        double lng2 = lng1 + Math.atan2(Math.sin(bearing) * Math.sin(angular) * Math.cos(lat1),
                Math.cos(angular) - Math.sin(lat1) * Math.sin(lat2));

        // 经度归一化到 -180 ~ 180
        double lngDegrees = Math.toDegrees(lng2);
        lngDegrees = (lngDegrees + 540.0) % 360.0 - 180.0;

        return new double[] {lngDegrees, Math.toDegrees(lat2)};
    }

    /** 两点之间的距离（米，Haversine） */
    public static double distanceMeters(double lng1, double lat1, double lng2, double lat2) {
        double radLng1 = Math.toRadians(lng1);
        double radLat1 = Math.toRadians(lat1);
        double radLng2 = Math.toRadians(lng2);
        double radLat2 = Math.toRadians(lat2);

        double sinLat = Math.sin((radLat2 - radLat1) / 2.0);
        double sinLng = Math.sin((radLng2 - radLng1) / 2.0);
        double h = sinLat * sinLat + Math.cos(radLat1) * Math.cos(radLat2) * sinLng * sinLng;

        return 2.0 * EARTH_RADIUS * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }
}
