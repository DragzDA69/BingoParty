/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2020, MasterKenth
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.dragz;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("Bingo Party by Dragz")
public interface BingoPartyConfig extends Config
{

//region Upload Configuration
	@ConfigSection(
			name = "Upload Configuration",
			description = "Plugin setup configuration.",
			position = 0
	)
	String configurationSection = "configurationSection";

	@ConfigItem(
			keyName = "webhookUrl",
			name = "Discord webhook URL(s)",
			description = "The Discord Webhook URL(s) to use, separated by newline",
			position = 1,
			section = configurationSection
	)
	default String webhookUrl()  { return ""; }

	@ConfigItem(
			keyName = "itemsListCode",
			name = "Items Pastebin Code",
			description = "Custom Pastebin code for the items paste you are wishing to use.",
			position = 2,
			section = configurationSection
	)
	default String itemsListCode() { return ""; }
//endregion


//region Message Options
	@ConfigSection(
			name = "Message Options",
			description = "Options for discord image and message upload.",
			position = 1
	)
	String messageOptsSection = "messageOptsSection";

	@ConfigItem(
			keyName = "sendScreenshot",
			name = "Send screenshot",
			description = "Whether to send a screenshot along with the message",
			position = 1,
			section = messageOptsSection
	)
	default boolean sendScreenshot()
	{
		return true;
	}

	@ConfigItem(
			keyName = "sendDropRateAndValue",
			name = "Send drop rate & value",
			description = "Whether to include the item's drop rate and value with the message",
			position = 2,
			section = messageOptsSection
	)
	default boolean sendDropRateAndValue()
	{
		return true;
	}
//endregion


//region Overlay Options
	@ConfigSection(
			name = "Keyword",
			description = "Options for keyword overlay.",
			position = 2
	)
	String keywordOverlaySection = "keywordOverlaySection";

	@ConfigItem(
			keyName = "enableOverlay",
			name = "Enable Overlay",
			description = "Display keyword overlay.",
			position = 1,
			section = keywordOverlaySection
	)
	default boolean enableOverlay() { return true; }

	@ConfigItem(
			keyName = "keyword",
			name = "Overlay Keyword",
			description = "",
			position = 2,
			section = keywordOverlaySection
	)
	default String overlayKeyword() { return ""; }

//endregion

}
