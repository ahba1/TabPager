package com.yhy.tpg.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.yhy.tpg.R;
import com.yhy.tpg.adapter.NavAdapter;
import com.yhy.tpg.cache.PagerCache;
import com.yhy.tpg.listener.OnPageChangedListener;
import com.yhy.tpg.pager.TpgFragment;
import com.yhy.tpg.utils.DensityUtils;
import com.yhy.tpg.widget.base.BadgeInterface;
import com.yhy.tpg.widget.base.TpgInterface;

import cn.bingoogolapple.badgeview.BGABadgeRadioButton;

/**
 * Created by HongYi Yan on 2017/3/11 23:02.
 */
public class NavView extends RelativeLayout implements TpgInterface, BadgeInterface {
    private ViewPager vpContent;
    private RadioGroup rgTabs;
    //是否拖动过ViewPager（用于区分是ViewPager联动RadioGroup还是RadioGroup联动ViewPager）
    private boolean mDragScrolledFlag = false;
    //是否是直接设置ViewPager页面
    private boolean isSetCurrentPagerFlag = false;
    //页面缓存
    private PagerCache mCache;
    //页面切换事件
    private OnPageChangedListener mPageChangedListener;
    //整个导航栏背景色，默认为#ffffff
    private int mNavBgColor;
    //整个导航栏背景图，默认为null
    private Drawable mNavBgImg;
    //导航栏默认字体颜色，默认为#000000
    private int mNavTextDefaultColor;
    //导航栏选中的字体颜色，默认为#000000
    private int mNavTextCheckedColor;
    //导航栏选中的背景色，默认为透明
    private int mNavBgCheckedColor;
    //导航栏选中的背景图，默认为null
    private Drawable mNavBgCheckedImg;

    public NavView(Context context) {
        this(context, null);
    }

    public NavView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NavView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    /**
     * 初始化一些属性
     *
     * @param attrs 属性集
     */
    private void init(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.NavViewAttrs);

        mNavBgColor = ta.getColor(R.styleable.NavViewAttrs_nav_bg_color, Color.WHITE);
        mNavBgImg = ta.getDrawable(R.styleable.NavViewAttrs_nav_bg_img);
        mNavTextDefaultColor = ta.getColor(R.styleable.NavViewAttrs_nav_text_default_color, Color
                .BLACK);
        mNavTextCheckedColor = ta.getColor(R.styleable.NavViewAttrs_nav_text_checked_color, Color
                .BLACK);
        mNavBgCheckedColor = ta.getColor(R.styleable.NavViewAttrs_nav_bg_checked_color,
                Color.TRANSPARENT);
        mNavBgCheckedImg = ta.getDrawable(R.styleable.NavViewAttrs_nav_bg_checked_img);

        ta.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        //获取控件
        View view = LayoutInflater.from(getContext()).inflate(R.layout.widget_nav, this);
        vpContent = (ViewPager) view.findViewById(R.id.vp_content);
        rgTabs = (RadioGroup) view.findViewById(R.id.rg_tabs);

        //设置整个导航栏的背景。如果同时设置了颜色和图片做背景，以图片为主
        rgTabs.setBackgroundColor(mNavBgColor);
        if (null != mNavBgImg) {
            rgTabs.setBackgroundDrawable(mNavBgImg);
        }
    }

    /**
     * 设置适配器
     *
     * @param adapter 适配器
     */
    public void setAdapter(NavAdapter adapter) {
        //动态生成菜单
        int pageCount = adapter.getCount();
        RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(RadioGroup.LayoutParams
                .WRAP_CONTENT, RadioGroup.LayoutParams.MATCH_PARENT, 1);
        //先移除所有的菜单项
        rgTabs.removeAllViews();
        for (int i = 0; i < pageCount; i++) {

            BGABadgeRadioButton tab = (BGABadgeRadioButton) LayoutInflater.from(getContext())
                    .inflate(R.layout
                            .view_nav_tab, null);
            tab.setLayoutParams(params);
            tab.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(adapter
                    .getTabIconId(i)), null, null);
            tab.setText(adapter.getPageTitle(i));
            tab.setTextColor(mNavTextDefaultColor);

            rgTabs.addView(tab, i);
        }

        //从适配器获取页面缓存
        mCache = adapter.getPagerCache();

        //设置ViewPager适配器
        vpContent.setAdapter(adapter);

        //设置默认选中的菜单选项
        rgTabs.check(rgTabs.getChildAt(vpContent.getCurrentItem()).getId());
        //设置选中项样式
        setTabStyle((BGABadgeRadioButton) rgTabs.getChildAt(vpContent.getCurrentItem()), true);

        //初始化事件
        initListener();

        //绑定适配器与BtgView，为了在适配器中能获取到BtgView中的某些数据，比如当前页面
        adapter.bindTpgView(this);
    }

    private void initListener() {
        //触摸事件，便于获取到是否是拖动的标识
        vpContent.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mDragScrolledFlag = true;
                return false;
            }
        });

        //页面切换事件
        vpContent.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int
                    positionOffsetPixels) {
                if (null != mPageChangedListener) {
                    mPageChangedListener.onPageScrolled(position, positionOffset,
                            positionOffsetPixels);
                }
            }

            @Override
            public void onPageSelected(int position) {
                //页面切换后设置按钮选中状态

                //如果是拖动ViewPager，就需要联动RadioGroup，并在联动后将该标识设置为初始值
                if (mDragScrolledFlag || isSetCurrentPagerFlag) {
                    //联动RadioGroup
                    rgTabs.check(rgTabs.getChildAt(position).getId());
                }

                TpgFragment page = mCache.getPager(position);
                if (null != page) {
                    page.shouldLoadData();
                }
                if (null != mPageChangedListener) {
                    mPageChangedListener.onPageSelected(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (null != mPageChangedListener) {
                    mPageChangedListener.onPageScrollStateChanged(state);
                }
            }
        });

        //RadioGroup选中事件
        rgTabs.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                //底部菜单点击改变时设置页面选中状态
                int currentIndex = getTabIndexByResId(checkedId);

                for (int i = 0; i < rgTabs.getChildCount(); i++) {
                    //设置相关值
                    setTabStyle(((BGABadgeRadioButton) rgTabs.getChildAt(i)), i == currentIndex);
                }

                //如果不是拖动ViewPager时，说明是直接点击导航跳转的页面，此时才联动ViewPager
                if (!mDragScrolledFlag) {
                    vpContent.setCurrentItem(currentIndex, true);
                }

                //将拖动的标识关闭
                mDragScrolledFlag = false;

                //将设置当前页面的标识关闭
                isSetCurrentPagerFlag = false;
            }
        });
    }

    /**
     * 设置页面切换监听器
     *
     * @param listener 页面切换监听器
     */
    public void setOnPageChangedListener(OnPageChangedListener listener) {
        mPageChangedListener = listener;
    }

    /**
     * 设置当前页
     *
     * @param index 页面索引
     */
    @Override
    public void setCurrentPager(int index) {
        //设置页面标识设为true
        isSetCurrentPagerFlag = true;
        rgTabs.check(rgTabs.getChildAt(index).getId());
    }

    /**
     * 获取当前页面
     */
    @Override
    public int getCurrentPager() {
        return vpContent.getCurrentItem();
    }

    @Override
    public void showCirclePointBadge(int index) {
        getTabByIndex(index).showCirclePointBadge();
    }

    @Override
    public void showTextBadge(int index, String badgeText) {
        getTabByIndex(index).showTextBadge(badgeText);
    }

    @Override
    public void hiddenBadge(int index) {
        getTabByIndex(index).hiddenBadge();
    }

    @Override
    public void showDrawableBadge(int index, Bitmap bitmap) {
        getTabByIndex(index).showDrawableBadge(bitmap);
    }

    @Override
    public boolean isShowBadge(int index) {
        return getTabByIndex(index).isShowBadge();
    }

    private void setTabStyle(RadioButton rb, boolean checked) {
        if (checked) {
            rb.setTextColor(mNavTextCheckedColor);
            if (null == mNavBgCheckedImg) {
                rb.setBackgroundColor(mNavBgCheckedColor);
            } else {
                rb.setBackgroundDrawable(mNavBgCheckedImg);
            }
        } else {
            rb.setTextColor(mNavTextDefaultColor);
            rb.setBackgroundColor(Color.TRANSPARENT);
            rb.setBackgroundDrawable(null);
        }
    }

    private int getTabIndexByResId(int resId) {
        for (int i = 0; i < rgTabs.getChildCount(); i++) {
            if (resId == rgTabs.getChildAt(i).getId()) {
                return i;
            }
        }
        return -1;
    }

    private BGABadgeRadioButton getTabByIndex(int index) {
        int count = rgTabs.getChildCount();
        if (index < 0 || index > count) {
            throw new IllegalArgumentException("The argument index must between 0 and " +
                    "RadioGroup's childCount");
        }
        return (BGABadgeRadioButton) rgTabs.getChildAt(index);
    }
}