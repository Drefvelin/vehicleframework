package net.tfminecraft.VehicleFramework.Util;

import net.tfminecraft.VehicleFramework.Enums.Input;
import net.tfminecraft.VehicleFramework.Enums.Keybind;

public class EnumDisplayConverter {

    public static String getInputDisplayName(Input input) {
        switch (input) {
            case THROTTLE_UP: return "Speed Up";
            case THROTTLE_DOWN: return "Speed Down";
            case TURN_LEFT: return "Turn Left";
            case TURN_RIGHT: return "Turn Right";
            case TURN_LEFT_LOCAL: return "Turn Left";
            case TURN_RIGHT_LOCAL: return "Turn Right";
            case SEAT_SELECTION: return "Seat Selection";
            case MOVE: return "Move";
            case FORWARD: return "Forward";
            case BACKWARD: return "Backward";
            case PITCH_DOWN: return "Pitch Down";
            case PITCH_UP: return "Pitch Up";
            case ROLL_LEFT: return "Roll Left";
            case ROLL_RIGHT: return "Roll Right";
            case WEAPON_UP: return "Weapon Aim Up";
            case WEAPON_DOWN: return "Weapon Aim Down";
            case WEAPON_LEFT: return "Weapon Aim Left";
            case WEAPON_RIGHT: return "Weapon Aim Right";
            case WEAPON_RELOAD: return "Weapon Reload";
            case WEAPON_SHOOT: return "Weapon Shoot";
            case WEAPON_RELOAD_AND_SHOOT: return "Reload & Shoot";
            case WEAPON_SWITCH: return "Switch Weapon";
            case LIGHTS: return "Toggle Lights";
            case HORN: return "Horn";
            case NONE: return "None";
            default: return input.toString();
        }
    }

    public static String getKeybindDisplayName(Keybind keybind) {
        switch (keybind) {
            case W: return "W";
            case A: return "A";
            case S: return "S";
            case D: return "D";
            case RIGHT_CLICK: return "Right Click";
            case LEFT_CLICK: return "Left Click";
            case SHIFT_RIGHT_CLICK: return "Shift + Right Click";
            case SHIFT_LEFT_CLICK: return "Shift + Left Click";
            case SPACE: return "Space";
            case SPACE_W: return "Space + W";
            case SPACE_A: return "Space + A";
            case SPACE_S: return "Space + S";
            case SPACE_D: return "Space + D";
            case SHIFT: return "Shift";
            case SHIFT_W: return "Shift + W";
            case SHIFT_A: return "Shift + A";
            case SHIFT_S: return "Shift + S";
            case SHIFT_D: return "Shift + D";
            default: return keybind.toString();
        }
    }
}

