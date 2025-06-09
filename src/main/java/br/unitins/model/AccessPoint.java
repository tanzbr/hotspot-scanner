package br.unitins.model;

import java.time.LocalDateTime;

public class AccessPoint {
    private String ssid;
    private String macAddress;
    private int qualityLink;
    private int signalLevel;
    private int channel;
    private double frequency;
    private long lastBeacon;
    private int beaconInterval;
    private String wifiSecurity;
    private LocalDateTime scanTime;

    public AccessPoint() {}

    public AccessPoint(String ssid, String macAddress, int qualityLink, int signalLevel, 
                      int channel, double frequency, long lastBeacon, int beaconInterval, 
                      String wifiSecurity) {
        this.ssid = ssid;
        this.macAddress = macAddress;
        this.qualityLink = qualityLink;
        this.signalLevel = signalLevel;
        this.channel = channel;
        this.frequency = frequency;
        this.lastBeacon = lastBeacon;
        this.beaconInterval = beaconInterval;
        this.wifiSecurity = wifiSecurity;
        this.scanTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getSsid() { return ssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public int getQualityLink() { return qualityLink; }
    public void setQualityLink(int qualityLink) { this.qualityLink = qualityLink; }

    public int getSignalLevel() { return signalLevel; }
    public void setSignalLevel(int signalLevel) { this.signalLevel = signalLevel; }

    public int getChannel() { return channel; }
    public void setChannel(int channel) { this.channel = channel; }

    public double getFrequency() { return frequency; }
    public void setFrequency(double frequency) { this.frequency = frequency; }

    public long getLastBeacon() { return lastBeacon; }
    public void setLastBeacon(long lastBeacon) { this.lastBeacon = lastBeacon; }

    public int getBeaconInterval() { return beaconInterval; }
    public void setBeaconInterval(int beaconInterval) { this.beaconInterval = beaconInterval; }

    public String getWifiSecurity() { return wifiSecurity; }
    public void setWifiSecurity(String wifiSecurity) { this.wifiSecurity = wifiSecurity; }

    public LocalDateTime getScanTime() { return scanTime; }
    public void setScanTime(LocalDateTime scanTime) { this.scanTime = scanTime; }

    @Override
    public String toString() {
        return String.format("%-20s | %-17s | %3d%% | %4ddBm | %2d | %.1fGHz | %8dms | %4dTUs | %s",
                ssid != null ? ssid : "Hidden",
                macAddress,
                qualityLink,
                signalLevel,
                channel,
                frequency,
                lastBeacon,
                beaconInterval,
                wifiSecurity != null ? wifiSecurity : "Unknown");
    }
} 