package net.tfminecraft.VehicleFramework.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.tfminecraft.VehicleFramework.Cache.Cache;

public class OwnerData {
    private String owner;
    private List<String> whiteList = new ArrayList<>();
    private boolean whiteListed = false;
    private boolean ticketsEnabled = false;
    private String ticketId;

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

    public boolean isTicketsEnabled() {
        return ticketsEnabled;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = blankToNull(ticketId);
    }

    public void setTicketsEnabled(boolean ticketsEnabled) {
        this.ticketsEnabled = ticketsEnabled;
        if (ticketsEnabled && this.ticketId == null) {
            this.ticketId = UUID.randomUUID().toString();
        }
    }

    public void toggleTickets() {
        setTicketsEnabled(!ticketsEnabled);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}