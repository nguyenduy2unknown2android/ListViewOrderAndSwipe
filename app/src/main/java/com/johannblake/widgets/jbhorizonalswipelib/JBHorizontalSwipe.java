package com.johannblake.widgets.jbhorizonalswipelib;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Created by Johann on 6/27/15.
 */
public class JBHorizontalSwipe
{
  private final String LOG_TAG = "JBHorizontalSwipe";
  private final String TAG_TOP_VIEW = "TopView";

  private boolean fingerUp;
  private float scrollDeltaX;
  private float scrollDeltaY;
  private float motionEventPrevX;
  private float motionEventPrevY;
  private boolean scrollingRight;
  private View vScroller;
  private IJBHorizontalSwipe ijbHorizontalSwipe;
  private ObjectAnimator animatorView;
  private boolean animating;
  private boolean cancelAnimation;
  private float initialLeft;
  private boolean topViewChanged;
  private boolean topViewVisible;

  public final static int ANIMATE_POSITION_LEFT_VISIBLE = 0;
  public final static int ANIMATE_POSITION_LEFT_INVISIBLE = 1;
  public final static int ANIMATE_POSITION_RIGHT_VISIBLE = 2;
  public final static int ANIMATE_POSITION_RIGHT_INVISIBLE = 3;

  public JBHorizontalSwipe(IJBHorizontalSwipe ijbHorizontalSwipe)
  {
    this.ijbHorizontalSwipe = ijbHorizontalSwipe;
  }


  /**
   * Receives motion events from the scroller. Scrollers must implement the dispatchTouchEvent method and call this
   * method from there.
   *
   * @param v     Indicates the scroller that sent the motion event.
   * @param event The motion event that was sent.
   */

  public void onScrollerDispatchTouchEventListener(View v, MotionEvent event)
  {
    try
    {
      if (event.getAction() == MotionEvent.ACTION_DOWN)
      {
        this.vScroller = v;
        View vTop = this.vScroller.findViewWithTag(TAG_TOP_VIEW);
        this.initialLeft = vTop.getX();
      }
    }
    catch (Exception ex)
    {
      Log.e(LOG_TAG, "onScrollerDispatchTouchEventListener: " + ex.toString());
    }
  }

  public void onRootDispatchTouchEventListener(MotionEvent event)
  {
    try
    {
      if (event.getAction() == MotionEvent.ACTION_UP)
      {
        // Reposition the top view if necessary.
        this.fingerUp = true;

        if (this.vScroller != null)
        {
          View vTop = this.vScroller.findViewWithTag(TAG_TOP_VIEW);

          if ((vTop != null) && (vTop.getX() != 0))
            processViewPosition(vTop);

          this.vScroller = null;
        }
      }
      else if (event.getAction() == MotionEvent.ACTION_DOWN)
      {
        this.fingerUp = false;
        this.motionEventPrevX = event.getX();
        this.motionEventPrevY = event.getY();
      }

      if ((event.getAction() == MotionEvent.ACTION_MOVE) && (this.vScroller != null))
      {
        // Adjust the position of the view.
        this.scrollingRight = event.getX() > this.motionEventPrevX;
        this.scrollDeltaX = Math.abs(event.getX() - this.motionEventPrevX);
        this.scrollDeltaY = Math.abs(event.getY() - this.motionEventPrevY);
        this.motionEventPrevX = event.getX();
        this.motionEventPrevY = event.getY();

        View vTop = this.vScroller.findViewWithTag(TAG_TOP_VIEW);

        if (((this.scrollDeltaX > 10) && (this.scrollDeltaY < 10)) || ((vTop != null) && (vTop.getX() != 0)))
        {
          IJBHorizontalSwipeTouch ijbHorizontalSwipeTouch = (IJBHorizontalSwipeTouch) this.vScroller.getParent();
          ijbHorizontalSwipeTouch.setDisableScrolling(true);

          ListView listview = (ListView) vTop.getParent().getParent();
          listview.requestDisallowInterceptTouchEvent(true);

          repositionTopView();
        }
      }
    }
    catch (Exception ex)
    {
      Log.e(LOG_TAG, "onRootDispatchTouchEventListener: " + ex.toString());
    }
  }


  private void repositionTopView()
  {
    try
    {
      View vTop = this.vScroller.findViewWithTag(TAG_TOP_VIEW);

      if (animating || fingerUp)
        return;

      if (this.scrollingRight)
      {
        float x = vTop.getX() + this.scrollDeltaX;

        if (vTop.getX() >= (vTop.getWidth() - 1))
          vTop.setX(-(vTop.getWidth() - 1));
        else
          vTop.setX(x);
      }
      else
      {
        float x = vTop.getX() - this.scrollDeltaX;

        if (vTop.getX() <= -(vTop.getWidth() - 1))
          vTop.setX(vTop.getWidth());
        else
          vTop.setX(x);
      }

      float alpha = (vTop.getWidth() - Math.abs(vTop.getX() - this.scrollDeltaX)) / vTop.getWidth();
      vTop.setAlpha(alpha);
    }
    catch (Exception ex)
    {
      Log.e(LOG_TAG, "repositionTopView: " + ex.toString());
    }
  }


  /**
   * This is where the decision is made to either display or hide the view.
   */
  private void processViewPosition(View vTop)
  {
    try
    {
      if (this.fingerUp)
      {
        if (this.vScroller == null)
          return;

        if (this.scrollingRight && (this.scrollDeltaX > 50) && (vTop.getX() > 0))
        {
          animateView(vTop, ANIMATE_POSITION_RIGHT_INVISIBLE);
          return;
        }

        if (this.scrollingRight && (this.scrollDeltaX > 50) && (vTop.getX() < 0))
        {
          animateView(vTop, ANIMATE_POSITION_RIGHT_VISIBLE);
          return;
        }

        if (!this.scrollingRight && (this.scrollDeltaX > 50) && (vTop.getX() > 0))
        {
          animateView(vTop, ANIMATE_POSITION_LEFT_VISIBLE);
          return;
        }

        if (!this.scrollingRight && (this.scrollDeltaX > 50) && (vTop.getX() < 0))
        {
          animateView(vTop, ANIMATE_POSITION_LEFT_INVISIBLE);
          return;
        }

        // View was moved to the right of its origin.
        if ((this.initialLeft == 0) && (vTop.getX() > 0) && (vTop.getX() < this.vScroller.getWidth() / 3))
        {
          animateView(vTop, ANIMATE_POSITION_LEFT_VISIBLE);
          return;
        }
        else if ((this.initialLeft == 0) && (vTop.getX() >= this.vScroller.getWidth() / 3))
        {
          animateView(vTop, ANIMATE_POSITION_RIGHT_INVISIBLE);
          return;
        }
        else if ((this.initialLeft == 0) && (vTop.getX() > -this.vScroller.getWidth() / 3))
        {
          animateView(vTop, ANIMATE_POSITION_RIGHT_VISIBLE);
          return;
        }
        else if (this.initialLeft == 0)
        {
          animateView(vTop, ANIMATE_POSITION_LEFT_INVISIBLE);
          return;
        }
        else if ((this.initialLeft > 0) && (vTop.getX() >= this.vScroller.getWidth() * 2 / 3))
        {
          animateView(vTop, ANIMATE_POSITION_RIGHT_INVISIBLE);
          return;
        }
        else if (this.initialLeft > 0)
        {
          animateView(vTop, ANIMATE_POSITION_LEFT_VISIBLE);
          return;
        }
        else if ((this.initialLeft < 0) && (vTop.getX() > -this.vScroller.getWidth() * 2 / 3))
        {
          animateView(vTop, ANIMATE_POSITION_RIGHT_VISIBLE);
          return;
        }
        else //if (vTop.getX() <= -this.vScroller.getWidth() / 3)
        {
          animateView(vTop, ANIMATE_POSITION_LEFT_INVISIBLE);
          return;
        }
      }
      else
      {
        if (this.animatorView != null)
          this.animatorView.cancel();
      }
    }
    catch (Exception ex)
    {
      Log.e(LOG_TAG, "processViewPosition: " + ex.toString());
    }
  }


  public void showTopView(View vTop)
  {
    try
    {
      if (vTop.getX() < 0)
        animateView(vTop, ANIMATE_POSITION_RIGHT_VISIBLE);
      else
        animateView(vTop, ANIMATE_POSITION_LEFT_VISIBLE);

      this.topViewVisible = true;
      this.topViewChanged = true;
    }
    catch (Exception ex)
    {
      Log.e(LOG_TAG, "showTopView: " + ex.toString());
    }
  }

  /**
   * Animates the view.
   */
  public void animateView(View vTop, int position)
  {
    try
    {
      if (this.animatorView != null)
        this.animatorView.cancel();

      float left;

      switch (position)
      {
        case ANIMATE_POSITION_LEFT_INVISIBLE:
          left = -vTop.getWidth();
          break;

        case ANIMATE_POSITION_RIGHT_INVISIBLE:
          left = vTop.getWidth();
          break;

        default:
          left = 0;
          break;
      }

      animating = true;
      PropertyValuesHolder pvhXBar = PropertyValuesHolder.ofFloat("x", vTop.getX(), left);
      this.animatorView = ObjectAnimator.ofPropertyValuesHolder(vTop, pvhXBar);
      this.animatorView.setInterpolator(new LinearInterpolator());
      this.animatorView.setDuration(200);
      this.animatorView.addListener(animListener);
      this.cancelAnimation = false;
      this.animatorView.start();

      if ((this.ijbHorizontalSwipe != null) && (left != this.initialLeft))
      {
        this.topViewChanged = true;
        this.topViewVisible = (position == ANIMATE_POSITION_LEFT_VISIBLE) || (position == ANIMATE_POSITION_RIGHT_VISIBLE);
      }
      else
        this.topViewChanged = false;

    }
    catch (Exception ex)
    {
      Log.e(LOG_TAG, "animateView: " + ex.toString());
    }
  }


  /**
   * The animation listener. Needed to know when the animation should be canceled.
   */
  private Animator.AnimatorListener animListener = new Animator.AnimatorListener()
  {
    @Override
    public void onAnimationStart(Animator animation)
    {
      if (cancelAnimation)
        animatorView.cancel();
    }

    @Override
    public void onAnimationEnd(Animator animation)
    {
      animating = false;
      cancelAnimation = false;
      View v = (View) animatorView.getTarget();
      v.setAlpha(1);

      if ((ijbHorizontalSwipe != null) && topViewChanged)
        ijbHorizontalSwipe.onTopViewVisibilityChange(v, topViewVisible);
    }

    @Override
    public void onAnimationCancel(Animator animation)
    {
      cancelAnimation = false;
    }

    @Override
    public void onAnimationRepeat(Animator animation)
    {
    }
  };


  public interface IJBHorizontalSwipe
  {
    void onReposition(float x, boolean scrollingRight, float scrollDelta);

    void onTopViewVisibilityChange(View vTop, boolean visible);
  }

  public interface IJBHorizontalSwipeTouch
  {
    void setDisableScrolling(boolean disable);
  }

  public interface IJBHorizontalSwipeAdapter
  {
    View getSelectedView();
  }
}
