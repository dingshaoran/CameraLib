camera android custom size 自定义大小

# CameraLib

由于相机拍照尺寸只能是 picturesize 数组中的一个，不能自定义大小，这里提供一个预览和拍照的位置完全对应，并且能自定义大小的工具类，（intent 带过来参数WIDTH，HEIGHT，PATH，NAME，默认为……）

如果以lib 库方式引入代码，请复制  <activity android:name="com.lib.camera.CameraActivity" >到清单文件

如果要以 demo 方式运行请去掉project.properties 文件中的 android.library=true 即可

如果要实现屏幕旋转功能 请在onSensorChanged 实现