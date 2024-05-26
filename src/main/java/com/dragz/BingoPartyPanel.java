package com.dragz;

//region Imports
import javax.inject.Inject;
import net.runelite.client.ui.PluginPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.ui.ColorScheme;
//endregion

public class BingoPartyPanel extends PluginPanel
{
    private final BingoPartyPlugin plugin;
    private final ClientThread clientThread;
    private List<String> displayItems;

    @Inject
    BingoPartyPanel(BingoPartyPlugin plugin, ClientThread clientThread)
    {
        this.plugin = plugin;
        this.clientThread = clientThread;
        this.displayItems = new ArrayList<>();
    }

    void SendRefresh(ActionEvent event)
    {
        clientThread.invoke(() -> {
            SetList(plugin.RefreshList());
        });
    }

    void SetList(List<String> list)
    {
        displayItems = list;
        RefreshUI();
    }

    public void ClearList()
    {
        displayItems.clear();
        RefreshUI();
    }

    private void RefreshUI()
    {
        removeAll();
        init();
    }

    private JLabel getHeader()
    {
        return new JLabel(
        "<html>" +
                "<center>" +
                    "<h2>Refresh List</h2>" +
                    "This will refresh the items list for the code entered in the config." +
                    "<br>" +
                    "<br>" +
                "</center>" +
            "</html>"
        );
    }

    private JLabel getListSection()
    {
        return new JLabel(
        "<html>" +
                "<center>" +
                    "<h3>Current List:</h3>" +
                    (displayItems.isEmpty()
                        ? "Please click the button below to load the items list."
                        : String.join(", ", displayItems)
                    ) +
                    "<br>" +
                    "<br>" +
                "</center>" +
            "</html>"
        );
    }


    void init()
    {
        getParent().setLayout(new BorderLayout());
        getParent().add(this, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel listContainer = new JPanel();
        listContainer.setLayout(new BorderLayout());
//        listContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JButton button = new JButton("Load List");
        button.addActionListener(this::SendRefresh);
        JPanel listFrame = new JPanel();
        listFrame.add(button);
        listContainer.add(listFrame, BorderLayout.PAGE_START);
        listContainer.add(getListSection(), BorderLayout.CENTER);

        add(getHeader(), BorderLayout.PAGE_START);
        add(listContainer, BorderLayout.CENTER);
//        add(getListSection(), BorderLayout.PAGE_END);
    }

    void init2()
    {
        getParent().setLayout(new BorderLayout());
        getParent().add(this, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel reloadContainer = new JPanel();
        reloadContainer.setLayout(new BorderLayout());
        reloadContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JButton button = new JButton("Reload Raid");

        JPanel reloadFrame = new JPanel();


        // btn action handler


        JLabel reloadMessage = new JLabel("<html><center><h3>Raid Reloader</h3>Reloading the raid will cause your client to disconnect temporarily.<br></center></html>");
        add(reloadMessage, BorderLayout.PAGE_START);
        add(reloadContainer, BorderLayout.CENTER);
    }



}
