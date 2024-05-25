package com.dragz;

import net.runelite.api.ChatMessageType;

public class ChatMessageData
{
    public ChatMessageData() { }
    public ChatMessageData(ChatMessageType _messageType, String _message)
    {
        messageType = _messageType;
        message = _message;
    }
    public ChatMessageData(ChatMessageType _messageType, String _name, String _message)
    {
        messageType = _messageType;
        name = _name;
        message = _message;
    }
    public ChatMessageType messageType;
    public String name = "";
    public String message;

    public static ChatMessageData NewGameMessage(String message)
    {
        return new ChatMessageData(ChatMessageType.GAMEMESSAGE, "", message);
    }
    public static ChatMessageData NewGameMessage(String name, String message)
    {
        return new ChatMessageData(ChatMessageType.GAMEMESSAGE, name, message);
    }

    public static ChatMessageData NewBroadcastMessage(String message)
    {
        return new ChatMessageData(ChatMessageType.BROADCAST, "", message);
    }
    public static ChatMessageData NewBroadcastMessage(String name, String message)
    {
        return new ChatMessageData(ChatMessageType.BROADCAST, name, message);
    }
}
