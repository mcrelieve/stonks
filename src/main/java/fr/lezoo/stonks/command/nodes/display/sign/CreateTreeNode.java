package fr.lezoo.stonks.command.nodes.display.sign;

import fr.lezoo.stonks.Stonks;
import fr.lezoo.stonks.command.objects.CommandTreeNode;
import fr.lezoo.stonks.command.objects.parameter.Parameter;
import fr.lezoo.stonks.listener.temp.SignDisplayEditionListener;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateTreeNode extends CommandTreeNode {
    public CreateTreeNode(CommandTreeNode parent) {
        super(parent, "create");

        addParameter(Parameter.QUOTATION_ID);
    }

    @Override
    public CommandResult execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command is for players only");
            return CommandResult.FAILURE;
        }

        if (args.length < 4)
            return CommandResult.THROW_USAGE;

        String quotationId = args[3].toLowerCase();
        if (!Stonks.plugin.quotationManager.has(quotationId)) {
            sender.sendMessage(ChatColor.RED + "Could not find a quotation with ID '" + quotationId + "'");
            return CommandResult.FAILURE;
        }

        new SignDisplayEditionListener(Stonks.plugin.quotationManager.get(quotationId), (Player) sender, false);
        sender.sendMessage(ChatColor.YELLOW + "Please click on a sign to register a new display sign");
        sender.sendMessage(ChatColor.YELLOW + "You can relog to cancel display sign creation");
        return CommandResult.SUCCESS;
    }
}
