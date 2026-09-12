package net.tfminecraft.VehicleFramework.Util;

public final class Text {
	private Text() {
	}

	public static String capitalize(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		char[] chars = str.toCharArray();
		boolean start = true;
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (Character.isWhitespace(c)) {
				start = true;
				continue;
			}
			if (start) {
				chars[i] = Character.toTitleCase(c);
				start = false;
			}
		}
		return new String(chars);
	}
}
