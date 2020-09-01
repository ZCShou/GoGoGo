package com.zcshou.joystick;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
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

	@SuppressLint("InflateParams")
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

	@SuppressLint("InflateParams")
	private void initButtonView() {
		isCenter = false;
		btnCenter = findViewById(R.id.btn_center);
		btnCenter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
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
				}

				if (mListener != null) {
					mListener.clickCenter();
				}
			}
		});

		isNorth = false;
		btnNorth = findViewById(R.id.btn_north);
		btnNorth.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
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
					} else {
						isNorth = false;
						btnNorth.setImageResource(R.drawable.ic_up);
						btnCenter.performClick();
					}
				}
				if (mListener != null) {
					mListener.clickTop();
				}
			}
		});

		isSouth = false;
		btnSouth = findViewById(R.id.btn_south);
		btnSouth.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
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
					} else {
						isSouth = false;
						btnSouth.setImageResource(R.drawable.ic_down);
						btnCenter.performClick();
					}
				}
				if (mListener != null) {
					mListener.clickBottom();
				}
			}
		});

		isWest = false;
		btnWest = findViewById(R.id.btn_west);
		btnWest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
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
					} else {
						isWest = false;
						btnWest.setImageResource(R.drawable.ic_left);
						btnCenter.performClick();
					}
				}
				if (mListener != null) {
					mListener.clickLeft();
				}
			}
		});

		isEast = false;
		btnEast = findViewById(R.id.btn_east);
		btnEast.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
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
					} else {
						isEast = false;
						btnEast.setImageResource(R.drawable.ic_right);
						btnCenter.performClick();
					}
				}
				if (mListener != null) {
					mListener.clickRight();
				}
			}
		});
	}

	public void setListener(ButtonViewClickListener mListener) {
		this.mListener = mListener;
	}

	public interface ButtonViewClickListener {
		/**
		 * 中间按钮被点击了
		 */
		void clickCenter();

		/**
		 * 左边按钮被点击了
		 */
		void clickLeft();

		/**
		 * 上边按钮被点击了
		 */
		void clickTop();

		/**
		 * 右边按钮被点击了
		 */
		void clickRight();

		/**
		 * 下边按钮被点击了
		 */
		void clickBottom();

	}

}
