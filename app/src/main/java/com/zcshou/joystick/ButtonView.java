package com.zcshou.joystick;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.zcshou.gogogo.R;

public class ButtonView extends LinearLayout {
    private ButtonViewClickListener mListener;
    private boolean isCenter = true;
    private ImageButton btnCenter;
    private boolean isNorth;
    private ImageButton btnNorth;
    private boolean isSouth;
    private ImageButton btnSouth;
    private boolean isWest;
    private ImageButton btnWest;
    private boolean isEast;
    private ImageButton btnEast;
    private boolean isEastNorth;
    private ImageButton btnEastNorth;
    private boolean isEastSouth;
    private ImageButton btnEastSouth;
    private boolean isWestNorth;
    private ImageButton btnWestNorth;
    private boolean isWestSouth;
    private ImageButton btnWestSouth;
    private final Context mContext;

    public ButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.joystick_button, this);

        initButtonView();
    }

    public ButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        LayoutInflater.from(context).inflate(R.layout.joystick_button, this);
        initButtonView();
    }

    public ButtonView(Context context) {
        super(context);
        mContext = context;

        LayoutInflater.from(context).inflate(R.layout.joystick_button, this);

        initButtonView();
    }

    private void initButtonView() {
        btnCenter = findViewById(R.id.btn_center);
        btnCenter.setOnClickListener(view -> {
            if (!isCenter) {
                isCenter = true;
                btnCenter.setImageResource(R.drawable.ic_lock_close);
                btnCenter.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));
            } else {
                isCenter = false;
                btnCenter.setImageResource(R.drawable.ic_lock_open);
                btnCenter.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));

                if (isNorth) {
                    isNorth = false;
                    btnNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                }
                if (isSouth) {
                    isSouth = false;
                    btnSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                }
                if (isWest) {
                    isWest = false;
                    btnWest.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                }
                if (isEast) {
                    isEast = false;
                    btnEast.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                }
                if (isEastNorth) {
                    isEastNorth = false;
                    btnEastNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                }
                if (isEastSouth) {
                    isEastSouth = false;
                    btnEastSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                }
                if (isWestNorth) {
                    isWestNorth = false;
                    btnWestNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                }
                if (isWestSouth) {
                    isWestSouth = false;
                    btnWestSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                }
                if (mListener != null) {
                    mListener.clickAngleInfo(false,0, 0);
                }
            }
        });
        /* 默认 */
        isCenter = true;
        btnCenter.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));

        isNorth = false;
        btnNorth = findViewById(R.id.btn_north);
        btnNorth.setOnClickListener(view -> {
            if (isCenter) {
                if (!isNorth) {
                    isNorth = true;
                    btnNorth.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));

                    isSouth = false;
                    btnSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWest = false;
                    btnWest.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEast = false;
                    btnEast.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEastNorth = false;
                    btnEastNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEastSouth = false;
                    btnEastSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWestNorth = false;
                    btnWestNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWestSouth = false;
                    btnWestSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    if (mListener != null) {
                        mListener.clickAngleInfo(true,90, 1);
                    }
                } else {
                    isNorth = false;
                    btnNorth.setImageResource(R.drawable.ic_up);
                    if (mListener != null) {
                        mListener.clickAngleInfo(false,90, 0);
                    }
                }
            } else {
                if (mListener != null) {
                    mListener.clickAngleInfo(false,90, 1);
                }
            }
        });

        isSouth = false;
        btnSouth = findViewById(R.id.btn_south);
        btnSouth.setOnClickListener(view -> {
            if (isCenter) {
                if (!isSouth) {
                    isSouth = true;
                    btnSouth.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));

                    isNorth = false;
                    btnNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWest = false;
                    btnWest.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEast = false;
                    btnEast.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEastNorth = false;
                    btnEastNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEastSouth = false;
                    btnEastSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWestNorth = false;
                    btnWestNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWestSouth = false;
                    btnWestSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));

                    if (mListener != null) {
                        mListener.clickAngleInfo(true,270, 1);
                    }
                } else {
                    isSouth = false;
                    btnSouth.setImageResource(R.drawable.ic_down);
                    if (mListener != null) {
                        mListener.clickAngleInfo(false,270, 0);
                    }
                }
            } else {
                if (mListener != null) {
                    mListener.clickAngleInfo(false,270, 1);
                }
            }
        });

        isWest = false;
        btnWest = findViewById(R.id.btn_west);
        btnWest.setOnClickListener(view -> {
            if (isCenter) {
                if (!isWest) {
                    isWest = true;
                    btnWest.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));

                    isSouth = false;
                    btnSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isNorth = false;
                    btnNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEast = false;
                    btnEast.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEastNorth = false;
                    btnEastNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEastSouth = false;
                    btnEastSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWestNorth = false;
                    btnWestNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWestSouth = false;
                    btnWestSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));

                    if (mListener != null) {
                        mListener.clickAngleInfo(true,180, 1);
                    }
                } else {
                    isWest = false;
                    btnWest.setImageResource(R.drawable.ic_left);
                    if (mListener != null) {
                        mListener.clickAngleInfo(false,180, 0);
                    }
                }
            } else {
                if (mListener != null) {
                    mListener.clickAngleInfo(false,180, 1);
                }
            }
        });

        isEast = false;
        btnEast = findViewById(R.id.btn_east);
        btnEast.setOnClickListener(view -> {
            if (isCenter) {
                if (!isEast) {
                    isEast = true;
                    btnEast.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));

                    isSouth = false;
                    btnSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isNorth = false;
                    btnNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWest = false;
                    btnWest.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEastNorth = false;
                    btnEastNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEastSouth = false;
                    btnEastSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWestNorth = false;
                    btnWestNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWestSouth = false;
                    btnWestSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));

                    if (mListener != null) {
                        mListener.clickAngleInfo(true,0, 1);
                    }
                } else {
                    isEast = false;
                    btnEast.setImageResource(R.drawable.ic_right);
                    if (mListener != null) {
                        mListener.clickAngleInfo(false,0, 0);
                    }
                }
            } else {
                if (mListener != null) {
                    mListener.clickAngleInfo(false,0, 1);
                }
            }
        });

        isEastNorth = false;
        btnEastNorth = findViewById(R.id.btn_north_east);
        btnEastNorth.setOnClickListener(view -> {
            if (isCenter) {
                if (!isEastNorth) {
                    isEastNorth = true;
                    btnEastNorth.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));

                    isSouth = false;
                    btnSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isNorth = false;
                    btnNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWest = false;
                    btnWest.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEast = false;
                    btnEast.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEastSouth = false;
                    btnEastSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWestNorth = false;
                    btnWestNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWestSouth = false;
                    btnWestSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));

                    if (mListener != null) {
                        mListener.clickAngleInfo(true,45, 1);
                    }
                } else {
                    isEastNorth = false;
                    btnEastNorth.setImageResource(R.drawable.ic_right_up);
                    if (mListener != null) {
                        mListener.clickAngleInfo(false,45, 0);
                    }
                }
            } else {
                if (mListener != null) {
                    mListener.clickAngleInfo(false,45, 1);
                }
            }
        });

        isEastSouth = false;
        btnEastSouth = findViewById(R.id.btn_south_east);
        btnEastSouth.setOnClickListener(view -> {
            if (isCenter) {
                if (!isEastSouth) {
                    isEastSouth = true;
                    btnEastSouth.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));

                    isSouth = false;
                    btnSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isNorth = false;
                    btnNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWest = false;
                    btnWest.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEast = false;
                    btnEast.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEastNorth = false;
                    btnEastNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWestNorth = false;
                    btnWestNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWestSouth = false;
                    btnWestSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));

                    if (mListener != null) {
                        mListener.clickAngleInfo(true,315, 1);
                    }
                } else {
                    isEastSouth = false;
                    btnEastSouth.setImageResource(R.drawable.ic_right_down);
                    if (mListener != null) {
                        mListener.clickAngleInfo(false,315, 0);
                    }
                }
            } else {
                if (mListener != null) {
                    mListener.clickAngleInfo(false,315, 1);
                }
            }
        });

        isWestNorth = false;
        btnWestNorth = findViewById(R.id.btn_north_west);
        btnWestNorth.setOnClickListener(view -> {
            if (isCenter) {
                if (!isWestNorth) {
                    isWestNorth = true;
                    btnWestNorth.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));

                    isSouth = false;
                    btnSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isNorth = false;
                    btnNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWest = false;
                    btnWest.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEast = false;
                    btnEast.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEastNorth = false;
                    btnEastNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEastSouth = false;
                    btnEastSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWestSouth = false;
                    btnWestSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));

                    if (mListener != null) {
                        mListener.clickAngleInfo(true,135, 1);
                    }
                } else {
                    isWestNorth = false;
                    btnWestNorth.setImageResource(R.drawable.ic_left_up);
                    if (mListener != null) {
                        mListener.clickAngleInfo(false,135, 0);
                    }
                }
            } else {
                if (mListener != null) {
                    mListener.clickAngleInfo(false,135, 1);
                }
            }
        });

        isWestSouth = false;
        btnWestSouth = findViewById(R.id.btn_south_west);
        btnWestSouth.setOnClickListener(view -> {
            if (isCenter) {
                if (!isWestSouth) {
                    isWestSouth = true;
                    btnWestSouth.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));

                    isSouth = false;
                    btnSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isNorth = false;
                    btnNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWest = false;
                    btnWest.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEast = false;
                    btnEast.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEastNorth = false;
                    btnEastNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isEastSouth = false;
                    btnEastSouth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                    isWestNorth = false;
                    btnWestNorth.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));

                    if (mListener != null) {
                        mListener.clickAngleInfo(true,225, 1);
                    }
                } else {
                    isWestSouth = false;
                    btnWestSouth.setImageResource(R.drawable.ic_left_down);
                    if (mListener != null) {
                        mListener.clickAngleInfo(false,225, 0);
                    }
                }
            } else {
                if (mListener != null) {
                    mListener.clickAngleInfo(false,225, 1);
                }
            }
        });
    }

    public void setListener(ButtonViewClickListener mListener) {
        this.mListener = mListener;
    }

    public interface ButtonViewClickListener {
        /**
         * 点击的角度信息
         */
        void clickAngleInfo(boolean auto, double angle, double r);
    }
}
