package ca.coffeeboys.heartbeat;

/**
 * Created by AlexLand on 2016-02-12.
 */
public interface PulseCallback {
    public void onPulse();

    public void onDataCollected(long pulseValue);
}
