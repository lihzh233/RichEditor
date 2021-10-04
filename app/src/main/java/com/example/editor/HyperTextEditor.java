package com.example.editor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;


public class HyperTextEditor extends ScrollView {

    /**所有子view的容器*/
    private LinearLayout layout;

    /**所有EditText的软键盘监听器*/
    private OnKeyListener keyListener;

    /**删除该图片，消失的图片控件索引*/
    private int disappearingImageIndex = 0;

    /**图片点击监听器*/
    private OnClickListener btnListener;

    /**所有EditText的焦点监听listener*/
    private OnFocusChangeListener focusListener;

    /**最近被聚焦的EditText*/
    private EditText lastFocusEdit;

    /**新生的view都会打一个tag，对每个view来说，这个tag是唯一的。*/
    private int viewTagIndex = 1;

    /**文字大小*/
    private int rtTextSize = 20;

    /**inflater对象*/
    private LayoutInflater inflater;

    private int screenWidth;

    public HyperTextEditor(Context context) {
        this(context, null);
    }

    public HyperTextEditor(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HyperTextEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflater = LayoutInflater.from(context);
        initLayoutView(context);
        initListener();
        initFirstEditText(context);
    }

    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    private void initLayoutView(Context context) {
        //初始化layout
        layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        //设置间距，防止生成图片时文字太靠边，不能用margin，否则有黑边
        //layout.setPadding(leftAndRight,topAndBottom,leftAndRight,topAndBottom);
        addView(layout, layoutParams);
    }

    private void initListener() {
        // 初始化键盘退格监听，主要用来处理点击回删按钮时，view的一些列合并操作
        keyListener = new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                //KeyEvent.KEYCODE_DEL    删除插入点之前的字符
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    EditText edit = (EditText) v;
                    //处于退格删除的逻辑
                    onBackspacePress(edit);
                }
                return false;
            }
        };

        focusListener = new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    lastFocusEdit = (EditText) v;
                }
            }
        };
    }

    private void initFirstEditText(Context context) {
        LinearLayout.LayoutParams firstEditParam = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        //int padding = HyperLibUtils.dip2px(context, EDIT_PADDING);
        EditText firstEdit = createEditText();
        layout.addView(firstEdit, firstEditParam);
        lastFocusEdit = firstEdit;
    }

    /**
     * 处理软键盘backSpace回退事件
     * @param editText 					光标所在的文本输入框
     */
    @SuppressLint("SetTextI18n")
    private void onBackspacePress(EditText editText) {
        if (editText==null){
            return;
        }
        int startSelection = editText.getSelectionStart();
        // 只有在光标已经顶到文本输入框的最前方，在判定是否删除之前的图片，或两个View合并
        if (startSelection == 0) {
            //获取当前控件在layout父容器中的索引
            int editIndex = layout.indexOfChild(editText);
            // 如果editIndex-1<0,
            View preView = layout.getChildAt(editIndex - 1);
            if (null != preView) {
                if (preView instanceof FrameLayout) {
                    // 光标EditText的上一个view对应的是图片，删除图片操作
                    ImageCloseClick(preView);
                } else if (preView instanceof EditText) {
                    // 光标EditText的上一个view对应的还是文本框EditText，删除文字操作
                    String str1 = editText.getText().toString();
                    EditText preEdit = (EditText) preView;
                    String str2 = preEdit.getText().toString();
                    // 合并文本view时，不需要transition动画
                    layout.setLayoutTransition(null);
                    //移除editText文本控件
                    layout.removeView(editText);
                    // 文本合并操作
                    preEdit.setText(str2 + str1);
                    preEdit.requestFocus();
                    preEdit.setSelection(str2.length(), str2.length());
                    lastFocusEdit = preEdit;
                }
            }
        }
    }

    /**处理图片上删除的点击事件*/
    public void ImageCloseClick(View view) {
        //获取当前要删除图片控件的索引值
        disappearingImageIndex = layout.indexOfChild(view);
        layout.removeView(view);
        //合并上下EditText内容
        mergeEditText();

    }

    /**图片删除的时候，如果上下方都是EditText，则合并处理*/
    private void mergeEditText() {
        View preView = layout.getChildAt(disappearingImageIndex - 1);
        View nextView = layout.getChildAt(disappearingImageIndex);
        if (preView instanceof EditText && nextView instanceof EditText) {
            EditText preEdit = (EditText) preView;
            EditText nextEdit = (EditText) nextView;
            String str1 = preEdit.getText().toString();
            String str2 = nextEdit.getText().toString();
            String mergeText = "";
            if (str2.length() > 0) {
                mergeText = str1 + "\n" + str2;
            } else {
                mergeText = str1;
            }

            layout.setLayoutTransition(null);
            layout.removeView(nextEdit);
            preEdit.setText(mergeText);
            //设置光标的定位
            preEdit.requestFocus();
            preEdit.setSelection(str1.length(), str1.length());
        }
    }


    /**添加生成文本输入框*/
    private EditText createEditText() {
        EditText editText = new EditText(getContext());
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        editText.setLayoutParams(layoutParams);
        editText.setTextSize(rtTextSize);
        editText.setTextColor(Color.parseColor("#616161"));
        editText.setCursorVisible(true);
        editText.setBackground(null);
        editText.setOnKeyListener(keyListener);
        editText.setOnFocusChangeListener(focusListener);
        editText.setTag(viewTagIndex++);
        //editText.setPadding(editNormalPadding, paddingTop, editNormalPadding, paddingTop);
        //editText.setHint(hint);
        //editText.setTextColor(rtTextColor);
        //editText.setHintTextColor(rtHintTextColor);
        //editText.setLineSpacing(rtTextLineSpace, 1.0f);
        //HyperLibUtils.setCursorDrawableColor(editText, cursorColor);
        return editText;
    }

    /**
     * 插入一张图片
     * @param imagePath							图片路径地址
     */
    public synchronized void insertImage(String imagePath) {
        //lastFocusEdit获取焦点的EditText
        String lastEditStr = lastFocusEdit.getText().toString();
        //获取光标所在位置
        int cursorIndex = lastFocusEdit.getSelectionStart();
        //获取光标前面的字符串
        String editStr1 = lastEditStr.substring(0, cursorIndex).trim();
        //获取光标后的字符串
        String editStr2 = lastEditStr.substring(cursorIndex).trim();
        //获取焦点的EditText所在位置
        int lastEditIndex = layout.indexOfChild(lastFocusEdit);
        if (lastEditStr.length() == 0 || editStr1.length() == 0) {
            addImageViewAtIndex(lastEditIndex, imagePath);
        } else if (editStr2.length() == 0) {
            // 如果光标已经顶在了editText的最末端，则需要添加新的imageView和EditText
            addEditTextAtIndex(lastEditIndex + 1, "");
            addImageViewAtIndex(lastEditIndex + 1, imagePath);
        } else {
            //如果光标已经顶在了editText的最中间，则需要分割字符串，分割成两个EditText，并在两个EditText中间插入图片
            //把光标前面的字符串保留，设置给当前获得焦点的EditText（此为分割出来的第一个EditText）
            lastFocusEdit.setText(editStr1);
            //把光标后面的字符串放在新创建的EditText中（此为分割出来的第二个EditText）
            addEditTextAtIndex(lastEditIndex + 1, editStr2);
            //在空的EditText的位置插入图片布局，空的EditText下移
            addImageViewAtIndex(lastEditIndex + 1, imagePath);
        }
        //隐藏小键盘
        hideKeyBoard();
    }

    /**隐藏小键盘*/
    private void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && lastFocusEdit != null) {
            imm.hideSoftInputFromWindow(lastFocusEdit.getWindowToken(), 0);
        }
    }

    /**
     * 在特定位置插入EditText
     * @param index							位置
     * @param editStr						EditText显示的文字
     */
    public synchronized void addEditTextAtIndex(final int index, CharSequence editStr) {
        EditText editText = createEditText();
        if (!TextUtils.isEmpty(editStr)) {
            //判断插入的字符串是否为空，如果没有内容则显示hint提示信息
            editText.setText(editStr);
        }

        layout.addView(editText, index);
        //插入新的EditText之后，修改lastFocusEdit的指向
        lastFocusEdit = editText;
        //获取焦点
        lastFocusEdit.requestFocus();
        //将光标移至文字指定索引处
        lastFocusEdit.setSelection(editStr.length(), editStr.length());
    }

    /**在特定位置添加ImageView*/
    public synchronized void addImageViewAtIndex(final int index, final String imagePath) {
        final FrameLayout imageLayout = createImageLayout();
        ImageView imageView = imageLayout.findViewById(R.id.edit_imageView);

        layout.addView(imageLayout, index);
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        if(width > screenWidth){
            ViewGroup.LayoutParams params = imageLayout.getLayoutParams();
            params.height = (int)((float)height * screenWidth / width);
            imageLayout.setLayoutParams(params);
        }
        imageView.setImageBitmap(bitmap);
    }

    /**生成图片View*/
    private FrameLayout createImageLayout() {
        FrameLayout layout = (FrameLayout) inflater.inflate(R.layout.hte_edit_imageview, null);
        layout.setTag(viewTagIndex++);
        return layout;
    }

}
