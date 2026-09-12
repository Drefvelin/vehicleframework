package net.tfminecraft.VehicleFramework.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TextTest {

	@Test
	void capitalize_titleCasesEachWord() {
		assertNull(Text.capitalize(null));
		assertEquals("", Text.capitalize(""));
		assertEquals("Hull", Text.capitalize("hull"));
		assertEquals("Steam Locomotive", Text.capitalize("steam locomotive"));
		assertEquals("Foo_bar", Text.capitalize("foo_bar"));
	}
}
