package net.playsi.Afkcam.client.command;

import net.minecraft.entity.EntityType;
import net.minecraft.text.*;
import net.minecraft.text.HoverEvent.*;
import net.minecraft.text.HoverEvent.Action;

import net.playsi.Afkcam.Afkcam;

import java.util.UUID;

public class CommandTextBuilder {

	private final String key;
	private final MutableText text;

	private CommandTextBuilder(String key, Object... args) {
		this.key  = key;
		this.text = CommandTextBuilder.translatable(key, args);
	}

	private static MutableText translatable(String key, Object... args) {
		for (int i = 0; i < args.length; ++i) {
			Object object = args[i];
			if (!isPrimitive(object) && !(object instanceof Text)) {
				args[i] = String.valueOf(object);
			}
		}

		return Text.literal(Afkcam.text(key, args).getString().replace("&", "§"));
	}

	private static boolean isPrimitive(Object object) {
		return object instanceof Number || object instanceof Boolean || object instanceof String;
	}

	public static CommandTextBuilder startBuilder(String key, Object... args) {
		return new CommandTextBuilder("command." + key, args);
	}

	public CommandTextBuilder withHoverEvent(HoverEvent hoverEvent) {
		Style style = this.text.getStyle().withHoverEvent(hoverEvent);
		this.text.setStyle(style);
		return this;
	}



	public CommandTextBuilder withClickEvent(ClickEvent clickEvent) {
		Style style = this.text.getStyle().withClickEvent(clickEvent);
		this.text.setStyle(style);
		return this;
	}

	public Text build() {
		return this.text;
	}
}
