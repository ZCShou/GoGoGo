package com.zcshou.joystick;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.zcshou.gogogo.R;

public class ButtonView extends LinearLayout {

	private ButtonViewClickListener mListener;
	boolean isCenter;
	ImageButton btnCenter;
	boolean isNorth;
	ImageButton btnNorth;
	boolean isSouth;
	ImageButton btnSouth;
	boolean isWest;
	ImageButton btnWest;
	boolean isEast;
	ImageButton btnEast;
	boolean isEastNorth;
	ImageButton btnEastNorth;
	boolean isEastSouth;
	ImageButton btnEastSouth;
	boolean isWestNorth;
	ImageButton btnWestNorth;
	boolean isWestSouth;
	ImageButton btnWestSouth;

	public ButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		LayoutInflater.from(context).inflate(R.layout.joystick_button, this);

		initButtonView();
	}

	public ButtonView(Context context, AttributeSet attrs) {
		super(context, attrs);

		LayoutInflater.from(context).inflate(R.layout.joystick_button, this);
		initButtonView();
	}

	public ButtonView(Context context) {
		super(context);

		LayoutInflater.from(context).inflate(R.layout.joystick_button, this);

		initButtonView();
	}

	private void initButtonView() {
		isCenter = false;
		btnCenter = findViewById(R.id.btn_center);
		btnCenter.setOnClickListener(view -> {
			if (!isCenter) {
				isCenter = true;
				btnCenter.setImageResource(R.drawable.ic_lock_close);
			} else {
				isCenter = false;
				btnCenter.setImageResource(R.drawable.ic_lock_open);

				if (isNorth) {
					isNorth = false;
					btnNorth.setImageResource(R.drawable.ic_up);
				}
				if (isSouth) {
					isSouth = false;
					btnSouth.setImageResource(R.drawable.ic_down);
				}
				if (isWest) {
					isWest = false;
					btnWest.setImageResource(R.drawable.ic_left);
				}
				if (isEast) {
					isEast = false;
					btnEast.setImageResource(R.drawable.ic_right);
				}
				if (isEastNorth) {
					isEastNorth = false;
					btnEastNorth.setImageResource(R.drawable.ic_right_up);
				}
				if (isEastSouth) {
					isEastSouth = false;
					btnEastSouth.setImageResource(R.drawable.ic_right_down);
				}
				if (isWestNorth) {
					isWestNorth = false;
					btnWestNorth.setImageResource(R.drawable.ic_left_up);
				}
				if (isWestSouth) {
					isWestSouth = false;
					btnWestSouth.setImageResource(R.drawable.ic_left_down);
				}
				if (mListener != null) {
					mListener.clickAngleInfo(false,0, 0);
				}
			}
		});

		isNorth = false;
		btnNorth = findViewById(R.id.btn_north);
		btnNorth.setOnClickListener(view -> {
			if (isCenter) {
				if (!isNorth) {
					isNorth = true;
					btnNorth.setImageResource(R.drawable.ic_up_pressed);

					isSouth = false;
					btnSouth.setImageResource(R.drawable.ic_down);
					isWest = false;
					btnWest.setImageResource(R.drawable.ic_left);
					isEast = false;
					btnEast.setImageResource(R.drawable.ic_right);
					isEastNorth = false;
					btnEastNorth.setImageResource(R.drawable.ic_right_up);
					isEastSouth = false;
					btnEastSouth.setImageResource(R.drawable.ic_right_down);
					isWestNorth = false;
					btnWestNorth.setImageResource(R.drawable.ic_left_up);
					isWestSouth = false;
					btnWestSouth.setImageResource(R.drawable.ic_left_down);
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
					btnSouth.setImageResource(R.drawable.ic_down_pressed);
					isNorth = false;
					btnNorth.setImageResource(R.drawable.ic_up);
					isWest = false;
					btnWest.setImageResource(R.drawable.ic_left);
					isEast = false;
					btnEast.setImageResource(R.drawable.ic_right);
					isEastNorth = false;
					btnEastNorth.setImageResource(R.drawable.ic_right_up);
					isEastSouth = false;
					btnEastSouth.setImageResource(R.drawable.ic_right_down);
					isWestNorth = false;
					btnWestNorth.setImageResource(R.drawable.ic_left_up);
					isWestSouth = false;
					btnWestSouth.setImageResource(R.drawable.ic_left_down);
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
					btnWest.setImageResource(R.drawable.ic_left_pressed);
					isNorth = false;
					btnNorth.setImageResource(R.drawable.ic_up);
					isSouth = false;
					btnSouth.setImageResource(R.drawable.ic_down);
					isEast = false;
					btnEast.setImageResource(R.drawable.ic_right);
					isEastNorth = false;
					btnEastNorth.setImageResource(R.drawable.ic_right_up);
					isEastSouth = false;
					btnEastSouth.setImageResource(R.drawable.ic_right_down);
					isWestNorth = false;
					btnWestNorth.setImageResource(R.drawable.ic_left_up);
					isWestSouth = false;
					btnWestSouth.setImageResource(R.drawable.ic_left_down);
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
					btnEast.setImageResource(R.drawable.ic_right_pressed);
					isNorth = false;
					btnNorth.setImageResource(R.drawable.ic_up);
					isSouth = false;
					btnSouth.setImageResource(R.drawable.ic_down);
					isWest = false;
					btnWest.setImageResource(R.drawable.ic_left);
					isEastNorth = false;
					btnEastNorth.setImageResource(R.drawable.ic_right_up);
					isEastSouth = false;
					btnEastSouth.setImageResource(R.drawable.ic_right_down);
					isWestNorth = false;
					btnWestNorth.setImageResource(R.drawable.ic_left_up);
					isWestSouth = false;
					btnWestSouth.setImageResource(R.drawable.ic_left_down);
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
					btnEastNorth.setImageResource(R.drawable.ic_right_up_pressed);

					isNorth = false;
					btnNorth.setImageResource(R.drawable.ic_up);
					isSouth = false;
					btnSouth.setImageResource(R.drawable.ic_down);
					isWest = false;
					btnWest.setImageResource(R.drawable.ic_left);
					isEast = false;
					btnEast.setImageResource(R.drawable.ic_right);
					isEastSouth = false;
					btnEastSouth.setImageResource(R.drawable.ic_right_down);
					isWestNorth = false;
					btnWestNorth.setImageResource(R.drawable.ic_left_up);
					isWestSouth = false;
					btnWestSouth.setImageResource(R.drawable.ic_left_down);
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
					btnEastSouth.setImageResource(R.drawable.ic_right_down_pressed);

					isNorth = false;
					btnNorth.setImageResource(R.drawable.ic_up);
					isSouth = false;
					btnSouth.setImageResource(R.drawable.ic_down);
					isWest = false;
					btnWest.setImageResource(R.drawable.ic_left);
					isEast = false;
					btnEast.setImageResource(R.drawable.ic_right);
					isEastNorth = false;
					btnEastNorth.setImageResource(R.drawable.ic_right_up);
					isWestNorth = false;
					btnWestNorth.setImageResource(R.drawable.ic_left_up);
					isWestSouth = false;
					btnWestSouth.setImageResource(R.drawable.ic_left_down);
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
					btnWestNorth.setImageResource(R.drawable.ic_left_up_pressed);

					isNorth = false;
					btnNorth.setImageResource(R.drawable.ic_up);
					isSouth = false;
					btnSouth.setImageResource(R.drawable.ic_down);
					isWest = false;
					btnWest.setImageResource(R.drawable.ic_left);
					isEast = false;
					btnEast.setImageResource(R.drawable.ic_right);
					isEastNorth = false;
					btnEastNorth.setImageResource(R.drawable.ic_right_up);
					isEastSouth = false;
					btnEastSouth.setImageResource(R.drawable.ic_right_down);
					isWestSouth = false;
					btnWestSouth.setImageResource(R.drawable.ic_left_down);
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
					btnWestSouth.setImageResource(R.drawable.ic_left_down_pressed);

					isNorth = false;
					btnNorth.setImageResource(R.drawable.ic_up);
					isSouth = false;
					btnSouth.setImageResource(R.drawable.ic_down);
					isWest = false;
					btnWest.setImageResource(R.drawable.ic_left);
					isEast = false;
					btnEast.setImageResource(R.drawable.ic_right);
					isEastNorth = false;
					btnEastNorth.setImageResource(R.drawable.ic_right_up);
					isEastSouth = false;
					btnEastSouth.setImageResource(R.drawable.ic_right_down);
					isWestNorth = false;
					btnWestNorth.setImageResource(R.drawable.ic_left_up);
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
		void clickAngleInfo(Boolean auto, double angle, double r);

	}

}
