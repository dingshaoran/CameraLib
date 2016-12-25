package com.lib.camera;

import java.io.File;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.lib.utils.FileUtils;
import com.lib.utils.ImageUtils;
import com.lib.utils.LogUtils;

/**
 * 所有的相机拍照页<br/>
 * 参数<br/>
 * MARK 拍图片带的一个标识<br/>
 * POSITION (int)拍摄哪张图片，最后一张直接返回，否则拍下一张<br/>
 * SIMPLE_IMAGES ( String[])拍照时的示例图片，在拍照地方的左上角。<br/>
 * FRAME_IMAGES ( String[])拍照时的蒙层<br/>
 * NAME ( String[])拍照显示图片的名字<br/>
 * TIME (String[])每张图片拍摄的时间<br/>
 * CODE (String[])每张图片拍摄的位置<br/>
 * DATA (String[])初始化的图片list，返回时带回去<br/>
 * URL (String[])初始化图片已上传的网址list<br/>
 * LOCATION (string)图片要存储到的文件夹<br/>
 * PICNAME (string)要存为的文件名，如：pic%d.jpg<br/>
 * FILTER 自定义操作<br/>
 * KEYWORD (string)识别图片为文字的map.tojson<br/>
 * NEXTACTIVITY (Class)下一个activity，null返回上一个界面<br/>
 * RECT (Rect)所拍的图片裁剪掉四周大小<br/>
 * POINT (Point)所拍照保存下来的图片大小(裁剪后的)<br/>
 * UPLOAD (boolean)是不是要上传到服务器<br/>
 * ROTATE (boolean)是否要旋转。<br/>
 * <br/>
 * 返回值<br/>
 * MARK (int)拍图片带的一个标识，<br/>
 * URL (String[])如果设置上传为true，则有值<br/>
 * PATH (String[])返回的图片存储路径，<br/>
 * KEYWORD识别出来返回的数据map.fromjson TIME (String[])每张图片拍摄的时间<br/>
 * CODE (String[])每张图片拍摄的位置<br/>
 */
@SuppressLint({ "ClickableViewAccessibility" })
public class CameraActivity extends Activity implements View.OnClickListener, SensorEventListener {
	public static final int ORIENTATION_UNKNOWN = -1;
	private static final String WIDTH = "width";
	private static final String HEIGHT = "height";
	private static final int _DATA_X = 0;
	private static final int _DATA_Y = 1;
	private static final int _DATA_Z = 2;
	public static final int DEFAUT_HEIGHT = 1066;
	public static final int DEFAUT_WIDTH = 1600;
	public static final int FILTER_ADD = 2;
	public static final int FILTER_NO = -1;
	private static final String PATH = "path";
	private static final String NAME = "name";
	private static final String TAG = "CameraActivity";
	private final int CAMERA_SIZE = Camera.getNumberOfCameras();
	private View tvRemake, tvCancel, tvSave, btnChange; // 重拍，保存,取消
	private ImageView ivPhoto; // 显示照片的背景
	private View btFlash;
	private TextView tvLeft, tvTop, tvBottom, tvRight;
	private SurfaceView svPreView; // 相机预览布局
	private Bitmap mBitmap;
	private View tvTakePhoto;
	private CameraHelper mCameraHelper;
	private int mCameraPos = 0;
	private int windowWidth;
	private int windowHeight;
	private int mDegreeTaken;
	private int dh;
	private int dw;
	private int mFilter;
	private int top_height;
	private int bottom_height;
	private int mWidth;
	private int mHeight;
	private String mPath;
	private String mName;
	private ShutterCallback shutterCallback;

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {// 去掉navigation
																	// bar
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
							| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
							| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_FULLSCREEN
							| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);// 去掉信息栏
		final View root = View.inflate(this, R.layout.activity_lib_camera, null);
		setContentView(root);
		svPreView = (SurfaceView) findViewById(R.id.svPreView);
		tvSave = findViewById(R.id.tvSave);
		ivPhoto = (ImageView) findViewById(R.id.ivPhoto);
		tvRemake = findViewById(R.id.tvRemake);
		tvCancel = findViewById(R.id.tvCancel);
		btFlash = findViewById(R.id.btFlash);
		btnChange = findViewById(R.id.btnChange);
		tvTakePhoto = findViewById(R.id.tvTakePhoto);
		tvLeft = (TextView) findViewById(R.id.tvLeft);
		tvTop = (TextView) findViewById(R.id.tvTop);
		tvRight = (TextView) findViewById(R.id.tvRight);
		tvBottom = (TextView) findViewById(R.id.tvBottom);
		tvSave.setOnClickListener(this);
		tvRemake.setOnClickListener(this);
		btFlash.setOnClickListener(this);
		btnChange.setOnClickListener(this);
		tvCancel.setOnClickListener(this);
		tvTakePhoto.setOnClickListener(this);
		mWidth = getIntent().getIntExtra(WIDTH, DEFAUT_WIDTH);
		mHeight = getIntent().getIntExtra(HEIGHT, DEFAUT_HEIGHT);
		mPath = getIntent().getStringExtra(PATH);
		mName = getIntent().getStringExtra(NAME);
		if (mPath == null) {
			mPath = FileUtils.getDir(this, "pic");
		}
		if (mName == null) {
			mName = System.currentTimeMillis() + ".jpg";
		}
		mCameraHelper = new CameraHelper(this);
		mCameraHelper.setSurface(svPreView, mWidth, mHeight);
		shutterCallback = new ShutterCallback();
		root.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {// 对 surface 可见区域遮挡，使宽高比等于要获取图片的
				root.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				float width = root.getWidth();
				float height = root.getHeight();
				LayoutParams lpTop = (LayoutParams) tvTop.getLayoutParams();
				LayoutParams lpBottom = (LayoutParams) tvBottom.getLayoutParams();
				LayoutParams lpLeft = (LayoutParams) tvLeft.getLayoutParams();
				LayoutParams lpRight = (LayoutParams) tvRight.getLayoutParams();
				if (mWidth / width > mHeight / height) {// 需要拍取的图片宽，当前的预览大小窄，压缩预览高度。
					float desireHeight = mHeight * width / mWidth;
					int dh = (int) ((height - desireHeight) / 2);
					lpTop.height = (dh);
					lpBottom.height = (dh);
				} else {// 压缩宽度
					float desireWidth = mWidth * height / mHeight;
					int dw = (int) ((width - desireWidth) / 2);
					lpLeft.width = (dw);
					lpRight.width = (dw);
				}
				tvTop.requestLayout();
				tvBottom.requestLayout();
				tvLeft.requestLayout();
				tvRight.requestLayout();
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		CameraHelper.registOrientation(this, this);
		mCameraHelper.openCamera(mCameraPos);// 官方文档说一定要在onResume 打开相机，在onPause
												// 关闭相机否则可能导致无法使用
		showSurface(true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		CameraHelper.unRegistOrientation(this, this);
		mCameraHelper.release();
		shutterCallback.release();
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.tvTakePhoto) {
			takePicture();
		} else if (id == R.id.tvRemake) {
			showSurface(true);
		} else if (id == R.id.tvSave) {
			savePicture();
		} else if (id == R.id.btFlash) {
			turnFlash();
		} else if (id == R.id.tvCancel) {
			onBackPressed();
		} else if (id == R.id.btnChange) {
			mCameraHelper.release();
			mCameraPos = CAMERA_SIZE - 1 - mCameraPos;
		}
	}

	/**
	 * 重新拍照，把各个view都初始化刚开始的状态
	 */
	private void showSurface(boolean show) {
		Camera camera = mCameraHelper.getCamera();
		if (show) {
			tvRemake.setVisibility(View.INVISIBLE);
			tvSave.setVisibility(View.INVISIBLE);
			btFlash.setVisibility(View.VISIBLE);
			btnChange.setVisibility(View.VISIBLE);
			tvTakePhoto.setVisibility(View.VISIBLE);
			tvCancel.setVisibility(View.VISIBLE);
			ivPhoto.setScaleType(ImageView.ScaleType.FIT_XY);
			ivPhoto.setBackgroundResource(android.R.color.transparent);
			if (camera != null) {
				camera.startPreview();
			}
			mBitmap = null;
		} else {
			btFlash.setVisibility(View.INVISIBLE);
			btnChange.setVisibility(View.INVISIBLE);
			tvCancel.setVisibility(View.INVISIBLE);
			tvTakePhoto.setVisibility(View.INVISIBLE);
			tvRemake.setVisibility(View.VISIBLE);
			tvSave.setVisibility(View.VISIBLE);
			ivPhoto.setScaleType(ImageView.ScaleType.FIT_XY);
			ivPhoto.setBackgroundResource(android.R.color.black);
			if (mBitmap != null) {
				ivPhoto.setImageBitmap(mBitmap);
			}
			if (camera != null) {
				camera.stopPreview();
			}
		}
	}

	public void takePicture() {
		try {
			mCameraHelper.getCamera().takePicture(shutterCallback, null, pictureCallback);
		} catch (Exception e) {
			LogUtils.e("CameraActivity", e);
			Toast.makeText(this, "相机调用失败请重新拍照", Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * 相机回调
	 */
	private final Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			try {
				camera.stopPreview();
				RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) svPreView.getLayoutParams();
				float left = tvLeft.getWidth() - layoutParams.leftMargin;
				float top = tvTop.getHeight() - layoutParams.topMargin;
				float rigtht = tvRight.getWidth() - layoutParams.leftMargin;
				float bottom = tvBottom.getHeight() - layoutParams.topMargin;
				Rect mode = new Rect((int) (left * mWidth / (layoutParams.width - left - rigtht) + 0.5f),
						(int) (top * mHeight / (layoutParams.height - top - bottom) + 0.5f),
						(int) (rigtht * mWidth / (layoutParams.width - left - rigtht) + 0.5f),
						(int) (bottom * mHeight / (layoutParams.height - top - bottom) + 0.5f));
				mBitmap = ImageUtils.byte2Bitmap(data, mWidth, mHeight, mode);// 裁剪图片，data
																				// 里是
																				// surfaceview
																				// 的整个区域，mode
																				// 是可见区域外的要裁剪掉的部分
				showSurface(false);
				savePicture();
				showSurface(true);
			} catch (Throwable e) {
				LogUtils.e("CameraActivity", e);
				showSurface(true);
			}
		}
	};

	private void savePicture() {
		try {
			FileUtils.writeFile(ImageUtils.getSmallBitmap(mBitmap, 200), mPath, mName, false);
		} catch (Throwable e) {
			LogUtils.e(TAG, e);
			Toast.makeText(this, "内存不足，保存失败", Toast.LENGTH_LONG).show();
			return;
		}
		String img = new File(mPath, mName).getAbsolutePath();
		String gallery = savePic2Gallery(img);
		Toast.makeText(this, gallery, Toast.LENGTH_LONG).show();
	}

	@Nullable
	private String savePic2Gallery(String img) {// 保存到相册，这个地方按照需要可以删掉，
		Uri uri = FileUtils.copyImage2Gallery(this, img);
		if (uri == null) {
			Toast.makeText(this, "保存失败", Toast.LENGTH_LONG).show();
			return null;
		}
		Cursor c = getContentResolver().query(uri, new String[] { MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME }, null, null, null);
		c.moveToFirst();
		String string = c.getString(0);
		String bucketDisplayName = c.getString(1);
		c.close();
		return string;
	}

	private void turnFlash() {
		Camera camera = mCameraHelper.getCamera();
		if (camera != null) {
			Camera.Parameters parameters = camera.getParameters();
			if (parameters == null) {
				return;
			}
			List<String> flashModes = parameters.getSupportedFlashModes();
			if (flashModes == null) {
				return;
			}
			if (!Camera.Parameters.FLASH_MODE_TORCH.equals(parameters.getFlashMode())) {
				btFlash.setBackgroundResource(R.drawable.camera_flash_on);
				if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
					parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
					camera.setParameters(parameters);
				}
			} else {
				btFlash.setBackgroundResource(R.drawable.camera_flash_off);
				if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
					parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
					camera.setParameters(parameters);
				}
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Camera camera = mCameraHelper.getCamera();
		if (camera != null) {
			Camera.Parameters parameters = camera.getParameters();
			int maxZoom = parameters.getMaxZoom();
			int zoom = parameters.getZoom();
			switch (keyCode) {
			case KeyEvent.KEYCODE_CAMERA:
				if (event.getRepeatCount() == 0) {
					if (mBitmap == null) {
						tvSave.performClick();
					} else {
						tvRemake.performClick();
					}
				}
				return true;
			case KeyEvent.KEYCODE_VOLUME_UP:
				zoom = (zoom + 2) < maxZoom ? (zoom + 2) : maxZoom;
				parameters.setZoom(zoom);
				camera.setParameters(parameters);
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				zoom = (zoom - 2) > 0 ? (zoom - 2) : 0;
				parameters.setZoom(zoom);
				camera.setParameters(parameters);
				return true;
			default:
				return super.onKeyDown(keyCode, event);
			}
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	/**
	 * 播放拍照的声音
	 */
	class ShutterCallback implements Camera.ShutterCallback {
		private ToneGenerator tone;

		@Override
		public void onShutter() {
			try {// init failed
				if (tone == null)
					tone = new ToneGenerator(AudioManager.AUDIOFOCUS_REQUEST_GRANTED, ToneGenerator.MIN_VOLUME);
				tone.startTone(ToneGenerator.TONE_PROP_BEEP);
			} catch (Exception e) {
				LogUtils.e("CameraActivity", e);
			}
		}

		public void release() {
			if (tone != null) {
				tone.release();
			}
		}
	};

	/**
	 * 如果要实现屏幕旋转，添加屏幕对应处理
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		float[] values = event.values;
		int orientation = ORIENTATION_UNKNOWN;
		float X = -values[_DATA_X];
		float Y = -values[_DATA_Y];
		float Z = -values[_DATA_Z];
		float magnitude = X * X + Y * Y;
		if (magnitude * 4 >= Z * Z) {
			float OneEightyOverPi = 57.29577957855f;
			float angle = (float) Math.atan2(-Y, X) * OneEightyOverPi;
			orientation = 90 - Math.round(angle);
			while (orientation >= 360) {
				orientation -= 360;
			}
			while (orientation < 0) {
				orientation += 360;
			}
			if (orientation > 45 && orientation < 135) {
				Log.i("270", "右横");
			} else if (orientation > 135 && orientation < 225) {
				Log.i("180", "倒屏");
			} else if (orientation > 225 && orientation < 315) {
				Log.i("90", "左横");
			} else if ((orientation > 315 && orientation < 360) || (orientation > 0 && orientation < 45)) {
				Log.i("0", "竖屏");
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
}
