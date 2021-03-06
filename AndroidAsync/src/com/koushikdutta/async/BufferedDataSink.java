package com.koushikdutta.async;

import java.nio.ByteBuffer;

import junit.framework.Assert;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.WritableCallback;

public class BufferedDataSink implements DataSink {
    DataSink mDataSink;
    public BufferedDataSink(DataSink datasink) {
        mDataSink = datasink;
        mDataSink.setWriteableCallback(new WritableCallback() {
            @Override
            public void onWriteable() {
                writePending();
                if (closePending) {
                    mDataSink.close();
                }
            }
        });
    }
    
    public boolean isBuffering() {
        return mPendingWrites != null;
    }
    
    public DataSink getDataSink() {
        return mDataSink;
    }

    private void writePending() {
//        Log.i("NIO", "Writing to buffer...");
        if (mPendingWrites != null) {
            mDataSink.write(mPendingWrites);
            if (mPendingWrites.remaining() == 0)
                mPendingWrites = null;
        }
        if (mPendingWrites == null && mWritable != null)
            mWritable.onWriteable();
    }
    
    ByteBufferList mPendingWrites;

    @Override
    public void write(ByteBuffer bb) {
        if (mPendingWrites == null)
            mDataSink.write(bb);

        if (bb.remaining() > 0) {
            int toRead = Math.min(bb.remaining(), mMaxBuffer);
            if (toRead > 0) {
                if (mPendingWrites == null)
                    mPendingWrites = new ByteBufferList();
                byte[] bytes = new byte[toRead];
                bb.get(bytes);
                mPendingWrites.add(ByteBuffer.wrap(bytes));
            }
        }
    }

    @Override
    public void write(ByteBufferList bb) {
        write(bb, false);
    }
    
    protected void write(ByteBufferList bb, boolean ignoreBuffer) {
        if (mPendingWrites == null)
            mDataSink.write(bb);

        if (bb.remaining() > 0) {
            int toRead = Math.min(bb.remaining(), mMaxBuffer);
            if (ignoreBuffer)
                toRead = bb.remaining();
            if (toRead > 0) {
                if (mPendingWrites == null)
                    mPendingWrites = new ByteBufferList();
                mPendingWrites.add(bb.get(toRead));
            }
        }
    }

    WritableCallback mWritable;
    @Override
    public void setWriteableCallback(WritableCallback handler) {
        mWritable = handler;
    }

    @Override
    public WritableCallback getWriteableCallback() {
        return mWritable;
    }
    
    public int remaining() {
        if (mPendingWrites == null)
            return 0;
        return mPendingWrites.remaining();
    }
    
    int mMaxBuffer = Integer.MAX_VALUE;
    public int getMaxBuffer() {
        return mMaxBuffer;
    }
    
    public void setMaxBuffer(int maxBuffer) {
        Assert.assertTrue(maxBuffer >= 0);
        mMaxBuffer = maxBuffer;
    }

    @Override
    public boolean isOpen() {
        return !closePending && mDataSink.isOpen();
    }

    boolean closePending;
    @Override
    public void close() {
        if (mPendingWrites != null) {
            closePending = true;
            return;
        }
        mDataSink.close();
    }

    @Override
    public void setClosedCallback(CompletedCallback handler) {
        mDataSink.setClosedCallback(handler);
    }

    @Override
    public CompletedCallback getClosedCallback() {
        return mDataSink.getClosedCallback();
    }

    @Override
    public AsyncServer getServer() {
        return mDataSink.getServer();
    }
}
