package org.mcteam.ancientgates.commands.base;

import org.mcteam.ancientgates.commands.BaseCommand;

public class CommandSetCoolDownEnable extends BaseCommand {
    public CommandSetCoolDownEnable() {
        aliases.add("setcooldownenable");

        requiredParameters.add("id");
        requiredParameters.add("enabled");

        requiredPermission = "ancientgates.setTime";

        senderMustBePlayer = false;

        helpDescription = "设置冷却开关(只有这个开关打开才能冷却)";
    }

    @Override
    public void perform() {
        gate.setCoolDownEnabled(Boolean.valueOf(parameters.get(1)));
        gate.save();
        sendMessage("set ok");
    }
}
