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

@ConfigGroup("bingoparty")
public interface BingoPartyConfig extends Config
{

	@ConfigItem(
			keyName = "webhookUrl",
			name = "Discord webhook URL(s)",
			description = "The Discord Webhook URL(s) to use, separated by newline",
			position = 1
	)
	default String webhookUrl()  { return ""; }


	@ConfigItem(
			keyName = "itemsListCode",
			name = "Pastebin Paste Code",
			description = "Custom Pastebin code for the items paste you are wishing to use.",
			position = 2
	)
	default String itemsListCode() { return ""; }



	@ConfigItem(
			keyName = "loadedItemsList",
			name = "Loaded Items List",
			description = "These are the items loaded from the pastebin code.",
			position = 3
	)
	default String loadedItemsList() { return ""; }



	@ConfigItem(
			keyName = "sendScreenshot",
			name = "Send screenshot",
			description = "Whether to send a screenshot along with the message",
			position = 4
	)
	default boolean sendScreenshot()
	{
		return true;
	}

}
