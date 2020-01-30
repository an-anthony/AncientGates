package org.mcteam.ancientgates.commands.base;

import org.mcteam.ancientgates.commands.BaseCommand;

public class CommandSetCoolDownTime extends BaseCommand {
    public CommandSetCoolDownTime() {
        aliases.add("setcooldowntime");

        requiredParameters.add("id");
        requiredParameters.add("time");

        requiredPermission = "ancientgates.setTime";

        senderMustBePlayer = false;

        helpDescription = "设置冷却时间(需要打开开关)";
    }

    @Override
    public void perform() {
        gate.setCoolDownTime(Long.valueOf(parameters.get(1)));
        gate.save();
        sendMessage("set ok");
    }
}
