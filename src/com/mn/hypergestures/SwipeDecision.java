package com.mn.hypergestures;

/** Pure state machine, coordinates in dp. Holding before movement is not a Recents gesture. */
public final class SwipeDecision {
    public static final int NONE=0, HOME=1, RECENTS=2, HORIZONTAL=3;
    private final float startX, startY;
    private float anchorX, anchorY;
    private long stableSince=-1;
    private boolean vertical, horizontal, cancelled;
    public SwipeDecision(float x, float y) { startX=x; startY=y; }
    public void move(float x, float y, long time) {
        if (cancelled || horizontal) return;
        float dx=x-startX, up=startY-y;
        if (!vertical) {
            if (Math.abs(dx)>=18 && Math.abs(dx)>Math.abs(up)*1.2f) { horizontal=true; return; }
            if (up>=18 && up>Math.abs(dx)*1.2f) vertical=true;
        }
        if (!vertical || up<60) { stableSince=-1; return; }
        if (stableSince<0 || Math.abs(y-anchorY)>12 || Math.abs(x-anchorX)>12) {
            stableSince=time; anchorX=x; anchorY=y;
        }
    }
    public int finish(float x,float y,long time) {
        move(x,y,time);
        if(cancelled) return NONE;
        if(horizontal) return Math.abs(x-startX)>=72 && Math.abs(x-startX)>Math.abs(startY-y)*1.4f ? HORIZONTAL : NONE;
        if(!vertical || startY-y<60 || Math.abs(x-startX)>startY-y) return NONE;
        return stableSince>=0 && time-stableSince>=350 ? RECENTS : HOME;
    }
    public long recentsDelay(long time){return !cancelled && vertical && stableSince>=0 ? Math.max(0,350-(time-stableSince)) : -1;}
    public boolean isVertical(){return vertical;}
    public boolean isHorizontal(){return horizontal;}
    public void cancel(){cancelled=true;}
}

