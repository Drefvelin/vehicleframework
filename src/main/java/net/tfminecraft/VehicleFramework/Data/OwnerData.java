package net.tfminecraft.VehicleFramework.Data;

import java.util.ArrayList;
import java.util.List;

import net.tfminecraft.VehicleFramework.Cache.Cache;

public class OwnerData {
    private String owner;
    private List<String> whiteList = new ArrayList<>();
    private boolean whiteListed = false;

    public OwnerData() {
        owner = "none";
        whiteListed = Cache.allowWhitelist && Cache.whitelistedByDefault;
    }

    // Owner
    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    // Whitelist
    public void setWhiteList(List<String> whiteList) {
        this.whiteList = whiteList;
    }

    public List<String> getWhiteList() {
        return whiteList;
    }

    // Add/remove helpers (optional but useful)
    public void addToWhiteList(String player) {
        this.whiteList.add(player);
    }

    public void removeFromWhiteList(String player) {
        this.whiteList.remove(player);
    }

    // Whitelisted toggle
    public void setWhiteListed(boolean whiteListed) {
        this.whiteListed = whiteListed;
    }

    public boolean isWhiteListed() {
        return whiteListed;
    }
}