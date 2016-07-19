package micro.douyu.com.weakhandler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class WeakHandler {
    private final Handler.Callback mCallback; // 内存缓存Callback
    private final ExecHandler mExec;
    private Lock mLock = new ReentrantLock();
    final DoubleWayList mRunnables = new DoubleWayList(mLock, null);

    ///////////////////////////////////////////////////////////////////////////
    // 构造方法
    ///////////////////////////////////////////////////////////////////////////

    public WeakHandler() {
        mCallback = null;
        mExec = new ExecHandler();
    }

    public WeakHandler(@Nullable Handler.Callback callback) {
        mCallback = callback; // Hard referencing body
        mExec = new ExecHandler(new WeakReference<>(callback));
    }

    public WeakHandler(@NonNull Looper looper) {
        mCallback = null;
        mExec = new ExecHandler(looper);
    }


    public WeakHandler(@NonNull Looper looper, @NonNull Handler.Callback callback) {
        mCallback = callback;
        mExec = new ExecHandler(looper, new WeakReference<>(callback));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Handler重写，CallBack弱引用,方便内存回收
    ///////////////////////////////////////////////////////////////////////////

    private static class ExecHandler extends Handler {
        private final WeakReference<Callback> mCallback;

        ExecHandler() {
            mCallback = null;
        }

        ExecHandler(@NonNull WeakReference<Callback> callback) {
            mCallback = callback;
        }

        ExecHandler(@Nullable Looper looper) {
            super(looper);
            mCallback = null;
        }

        ExecHandler(@Nullable Looper looper, @Nullable WeakReference<Callback> callback) {
            super(looper);
            mCallback = callback;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (mCallback == null) {
                return;
            }
            final Callback callback = mCallback.get();
            if (callback == null) { // Already disposed
                return;
            }
            callback.handleMessage(msg);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Runnable重写，保存Runnable 和 其链表引用关系
    ///////////////////////////////////////////////////////////////////////////
    static class WeakRunnable implements Runnable {
        private final WeakReference<Runnable> mDelegate;
        private final WeakReference<DoubleWayList> mReference;

        WeakRunnable(WeakReference<Runnable> delegate, WeakReference<DoubleWayList> reference) {
            mDelegate = delegate;
            mReference = reference;
        }

        @Override
        public void run() {
            final Runnable delegate = mDelegate.get();
            final DoubleWayList reference = mReference.get();
            if (reference != null) {
                reference.remove();
            }
            if (delegate != null) {
                delegate.run();
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // 工具方法
    ///////////////////////////////////////////////////////////////////////////
    private WeakRunnable wrapRunnable(@NonNull Runnable r) {
        //noinspection ConstantConditions
        if (r == null) {
            throw new NullPointerException("Runnable can't be null");
        }
        final DoubleWayList hardRef = new DoubleWayList(mLock, r);
        mRunnables.insertAfter(hardRef);
        return hardRef.wrapper;
    }

    //双向链表封装
    //此处如果用LinkList(线程安全的)也可以的，只是得保存好runnable与wrapper之间的一一对应关系，方便后面进行删除操作
    static class DoubleWayList {
        @Nullable
        DoubleWayList next;
        @Nullable
        DoubleWayList prev;
        @NonNull
        final Runnable runnable;
        @NonNull
        final WeakRunnable wrapper;

        @NonNull
        Lock lock;

        public DoubleWayList(@NonNull Lock lock, @NonNull Runnable r) {
            this.runnable = r;
            this.lock = lock;
            this.wrapper = new WeakRunnable(new WeakReference<>(r), new WeakReference<>(this));
        }

        public WeakRunnable remove() {
            lock.lock();
            try {
                if (prev != null) {
                    prev.next = next;
                }
                if (next != null) {
                    next.prev = prev;
                }
                prev = null;
                next = null;
            } finally {
                lock.unlock();
            }
            return wrapper;
        }

        public void insertAfter(@NonNull DoubleWayList candidate) {
            lock.lock();
            try {
                if (this.next != null) {
                    this.next.prev = candidate;
                }

                candidate.next = this.next;
                this.next = candidate;
                candidate.prev = this;
            } finally {
                lock.unlock();
            }
        }

        @Nullable
        public WeakRunnable remove(Runnable obj) {
            lock.lock();
            try {
                DoubleWayList curr = this.next; // Skipping head
                while (curr != null) {
                    if (curr.runnable == obj) { // We do comparison exactly how Handler does inside
                        return curr.remove();
                    }
                    curr = curr.next;
                }
            } finally {
                lock.unlock();
            }
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Handler方法复写
    ///////////////////////////////////////////////////////////////////////////

    public final boolean post(@NonNull Runnable r) {
        return mExec.post(wrapRunnable(r));
    }


    public final boolean postAtTime(@NonNull Runnable r, long uptimeMillis) {
        return mExec.postAtTime(wrapRunnable(r), uptimeMillis);
    }

    public final boolean postAtTime(@NonNull Runnable r, @NonNull Object token, long uptimeMillis) {
        return mExec.postAtTime(wrapRunnable(r), token, uptimeMillis);
    }

    public final boolean postDelayed(@NonNull Runnable r, long delayMillis) {
        return mExec.postDelayed(wrapRunnable(r), delayMillis);
    }

    public final boolean postAtFrontOfQueue(@NonNull Runnable r) {
        return mExec.postAtFrontOfQueue(wrapRunnable(r));
    }

    public final void removeCallbacks(@NonNull Runnable r) {
        final WeakRunnable runnable = mRunnables.remove(r);
        if (runnable != null) {
            mExec.removeCallbacks(runnable);
        }
    }

    public final void removeCallbacks(@NonNull Runnable r, @NonNull Object token) {
        final WeakRunnable runnable = mRunnables.remove(r);
        if (runnable != null) {
            mExec.removeCallbacks(runnable, token);
        }
    }

    public final boolean sendMessage(@NonNull Message msg) {
        return mExec.sendMessage(msg);
    }

    public final boolean sendEmptyMessage(int what) {
        return mExec.sendEmptyMessage(what);
    }

    public final boolean sendEmptyMessageDelayed(int what, long delayMillis) {
        return mExec.sendEmptyMessageDelayed(what, delayMillis);
    }

    public final boolean sendEmptyMessageAtTime(int what, long uptimeMillis) {
        return mExec.sendEmptyMessageAtTime(what, uptimeMillis);
    }

    public final boolean sendMessageDelayed(@NonNull Message msg, long delayMillis) {
        return mExec.sendMessageDelayed(msg, delayMillis);
    }

    public boolean sendMessageAtTime(@NonNull Message msg, long uptimeMillis) {
        return mExec.sendMessageAtTime(msg, uptimeMillis);
    }

    public final boolean sendMessageAtFrontOfQueue(@NonNull Message msg) {
        return mExec.sendMessageAtFrontOfQueue(msg);
    }

    public final void removeMessages(int what) {
        mExec.removeMessages(what);
    }


    public final void removeMessages(int what, Object object) {
        mExec.removeMessages(what, object);
    }

    public final void removeCallbacksAndMessages(@NonNull Object token) {
        mExec.removeCallbacksAndMessages(token);
    }

    public final boolean hasMessages(int what) {
        return mExec.hasMessages(what);
    }

    public final boolean hasMessages(int what, @NonNull Object object) {
        return mExec.hasMessages(what, object);
    }

    public final Looper getLooper() {
        return mExec.getLooper();
    }
}
