package net.tfminecraft.VehicleFramework.Protocol;

import java.util.ArrayList;
import java.util.List;

import net.tfminecraft.VehicleFramework.Enums.Keybind;

public class PacketConverter {
	public List<Keybind> convert(float sideways, float forward, boolean space, boolean sneak) {
	    List<Keybind> list = new ArrayList<>(5);

	    if (sneak) {
	        if (sideways > 0) list.add(Keybind.SHIFT_A);
	        if (sideways < 0) list.add(Keybind.SHIFT_D);
	        if (forward > 0) list.add(Keybind.SHIFT_W);
	        if (forward < 0) list.add(Keybind.SHIFT_S);
	        list.add(Keybind.SHIFT);
	        if (space) list.add(Keybind.SPACE);
	    } else if (space) {
	        if (sideways > 0) list.add(Keybind.SPACE_A);
	        if (sideways < 0) list.add(Keybind.SPACE_D);
	        if (forward > 0) list.add(Keybind.SPACE_W);
	        if (forward < 0) list.add(Keybind.SPACE_S);
	        list.add(Keybind.SPACE);
	        if (sneak) list.add(Keybind.SHIFT);
	    } else {
	        if (sideways > 0) list.add(Keybind.A);
	        if (sideways < 0) list.add(Keybind.D);
	        if (forward > 0) list.add(Keybind.W);
	        if (forward < 0) list.add(Keybind.S);
	    }

	    return list;
	}
}
