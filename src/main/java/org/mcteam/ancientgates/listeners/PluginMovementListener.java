package org.mcteam.ancientgates.listeners;

import java.util.Calendar;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.mcteam.ancientgates.Conf;
import org.mcteam.ancientgates.Gate;
import org.mcteam.ancientgates.Gates;
import org.mcteam.ancientgates.Plugin;
import org.mcteam.ancientgates.util.BlockUtil;
import org.mcteam.ancientgates.util.ExecuteUtil;
import org.mcteam.ancientgates.util.TeleportUtil;
import org.mcteam.ancientgates.util.types.WorldCoord;

public class PluginMovementListener implements Listener {

	public Plugin plugin;

	public PluginMovementListener(final Plugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMove(final PlayerMoveEvent event) {
		final Player player = event.getPlayer();

		// Check player is not carrying a passenger
		if (player.getPassenger() != null)
			return;

		final Location from = event.getFrom();
		final Location to = event.getTo();
		final Block blockTo = to.getBlock();

		if (!BlockUtil.canPlayerStandInGateBlock(blockTo, from.getBlockY() == to.getBlockY()))
			return;

		// Ok so a player walks into a portal block
		// Find the nearest gate!
		final WorldCoord playerCoord = new WorldCoord(player.getLocation());
		final Gate nearestGate = Gates.gateFromAll(playerCoord);

		if (nearestGate != null) {

			// Get current time
			final Long now = Calendar.getInstance().getTimeInMillis();

			// Before we change the sources, we should use some variables to instead of we need use cycle.
			final Boolean lastContain = Plugin.lastTeleportTime.containsKey(player.getName());
			final Long lastTeleportTime = lastContain ? Plugin.lastTeleportTime.get(player.getName()) : Long.valueOf(0);

			// Check player has passed cooldown period
			if ( lastContain && lastTeleportTime > now - Conf.getGateCooldownMillis())
				return;

			// Okay, We should check each gate `cooldown` time
			if(lastContain && nearestGate.getCoolDownEnabled() && lastTeleportTime != Long.valueOf(0)){
				// First, Check permission(s) player have that can ignore cool-time limited
				// Mr. Pikachu said: yamero!
				// And then,
				Long coolTime = nearestGate.getCoolDownTime();
				// Limit Player use time
				if(coolTime < 0){
					//if(coolTime == -1){
					player.sendMessage("§9GLaDos §f>> §b" + "你知道的, 这个门只能使用一次");
					Plugin.lastMessageTime.put(player.getName(), now);
					return;
					//}
				}else{
					coolTime = (lastTeleportTime + coolTime * 1000 - now) / 1000;
					if(coolTime > 0){
						//需要冷却
						if(Plugin.lastMessageTime.get(player.getName()) < now - 10000L){
							player.sendMessage("§9GLaDos §f>> §b" + String.format("你恐怕还得等上%d秒才能再次进入这个测试。",coolTime));
							Plugin.lastMessageTime.put(player.getName(), now);
						}
						return;
					}
				}
			}

			// Check player has permission to enter the gate.
			if (!Plugin.hasPermManage(player, "ancientgates.use." + nearestGate.getId()) && !Plugin.hasPermManage(player, "ancientgates.use.*") && Conf.enforceAccess) {
				if (!Plugin.lastMessageTime.containsKey(player.getName()) || Plugin.lastMessageTime.get(player.getName()) < now - 10000L) {
					player.sendMessage("§9GLaDos §f>> §b你不应该进入这个测试容器");
					Plugin.lastMessageTime.put(player.getName(), now);
				}
				return;
			}

			// Handle economy (check player has funds to use gate)
			if (!Plugin.handleEconManage(player, nearestGate.getCost())) {
				if (!Plugin.lastMessageTime.containsKey(player.getName()) || Plugin.lastMessageTime.get(player.getName()) < now - 10000L) {
					player.sendMessage("§9Wheatley §f>> §b抱歉伙计，你现在不配进门。等你有" + nearestGate.getCost() + "块钱的时候你才能进");
					Plugin.lastMessageTime.put(player.getName(), now);
				}
				return;
			}

			// Handle BungeeCord gates (BungeeCord support disabled)
			if (nearestGate.getBungeeTo() != null && Conf.bungeeCordSupport == false) {
				if (!Plugin.lastMessageTime.containsKey(player.getName()) || Plugin.lastMessageTime.get(player.getName()) < now - 10000L) {
					player.sendMessage(String.format("BungeeCord support not enabled."));
					Plugin.lastMessageTime.put(player.getName(), now);
				}
				return;
			}

			// Handle gates that do not point anywhere
			if (nearestGate.getTo() == null && nearestGate.getBungeeTo() == null && nearestGate.getCommand() == null) {
				if (!Plugin.lastMessageTime.containsKey(player.getName()) || Plugin.lastMessageTime.get(player.getName()) < now - 10000L) {
					player.sendMessage("§9GLaDos §f>> §b这个测试容器正在建设中");
					Plugin.lastMessageTime.put(player.getName(), now);
				}
				return;
			}

			// Teleport the player (Instant method)
			if (nearestGate.getTo() != null) {
				TeleportUtil.teleportPlayer(player, nearestGate.getTo(), nearestGate.getTeleportEntities(), nearestGate.getTeleportInventory());

				if (nearestGate.getCommand() != null) {
					ExecuteUtil.execCommand(player, nearestGate.getCommand(), nearestGate.getCommandType());
				}
				if (nearestGate.getMessage() != null) {
					player.sendMessage(nearestGate.getMessage());
				}

				Plugin.lastTeleportTime.put(player.getName(), now);
			} else if (nearestGate.getBungeeTo() != null) {
				TeleportUtil.teleportPlayer(player, nearestGate.getBungeeTo(), nearestGate.getBungeeType(), nearestGate.getTeleportEntities(), nearestGate.getTeleportInventory(), from.getBlockY() == to.getBlockY(), nearestGate.getCommand(), nearestGate.getCommandType(), nearestGate.getMessage());
			} else {
				ExecuteUtil.execCommand(player, nearestGate.getCommand(), nearestGate.getCommandType(), true);
				Plugin.lastTeleportTime.put(player.getName(), now);
			}
		}
	}

	@EventHandler
	public void onVehicleMove(final VehicleMoveEvent event) {
		final Location from = event.getFrom();
		final Location to = event.getTo();
		final Block blockTo = to.getBlock();

		if (!BlockUtil.canPlayerStandInGateBlock(blockTo, from.getBlockY() == to.getBlockY()))
			return;

		final Vehicle vehicle = event.getVehicle();
		final Entity passenger = vehicle.getPassenger();

		// Ok so a vehicle drives into a portal block
		// Find the nearest gate!
		final WorldCoord toCoord = new WorldCoord(event.getTo());
		final Gate nearestGate = Gates.gateFromAll(toCoord);

		if (nearestGate != null) {
			Long now = 0L;

			if (passenger instanceof Player) {
				final Player player = (Player) passenger;

				// Get current time
				now = Calendar.getInstance().getTimeInMillis();

				// Check player has passed cooldown period
				if (Plugin.lastTeleportTime.containsKey(player.getName()) && Plugin.lastTeleportTime.get(player.getName()) > now - Conf.getGateCooldownMillis())
					return;

				// Check player has permission to enter the gate.
				if (!Plugin.hasPermManage(player, "ancientgates.use." + nearestGate.getId()) && !Plugin.hasPermManage(player, "ancientgates.use.*") && Conf.enforceAccess) {
					if (!Plugin.lastMessageTime.containsKey(player.getName()) || Plugin.lastMessageTime.get(player.getName()) < now - 10000L) {
						player.sendMessage("§9GLaDos §f>> §b你不应该进入这个测试容器");
						Plugin.lastMessageTime.put(player.getName(), now);
					}
					return;
				}

				// Handle economy (check player has funds to use gate)
				if (!Plugin.handleEconManage(player, nearestGate.getCost())) {
					if (!Plugin.lastMessageTime.containsKey(player.getName()) || Plugin.lastMessageTime.get(player.getName()) < now - 10000L) {
						player.sendMessage("§9Wheatley §f>> §b抱歉伙计，你现在不配进门。等你有" + nearestGate.getCost() + "块钱的时候你才能进。");
						Plugin.lastMessageTime.put(player.getName(), now);
					}
					return;
				}

				// Handle BungeeCord gates (BungeeCord support disabled)
				if (nearestGate.getBungeeTo() != null && Conf.bungeeCordSupport == false) {
					if (!Plugin.lastMessageTime.containsKey(player.getName()) || Plugin.lastMessageTime.get(player.getName()) < now - 10000L) {
						player.sendMessage(String.format("BungeeCord support not enabled."));
						Plugin.lastMessageTime.put(player.getName(), now);
					}
					return;
				}

				// Handle gates that do not point anywhere
				if (nearestGate.getTo() == null && nearestGate.getBungeeTo() == null && nearestGate.getCommand() == null) {
					if (!Plugin.lastMessageTime.containsKey(player.getName()) || Plugin.lastMessageTime.get(player.getName()) < now - 10000L) {
						player.sendMessage("§9GLaDos §f>> §b这个测试容器正在建设中");
						Plugin.lastMessageTime.put(player.getName(), now);
					}
					return;
				}

			} else if (passenger != null) {
				if (!nearestGate.getTeleportEntities())
					return;
			}
			// Teleport the vehicle (with its passenger)
			if (nearestGate.getTeleportVehicles()) {
				if (nearestGate.getTo() != null) {
					TeleportUtil.teleportVehicle(vehicle, nearestGate.getTo(), nearestGate.getTeleportEntities(), nearestGate.getTeleportInventory());

					if (passenger instanceof Player && nearestGate.getCommand() != null) {
						ExecuteUtil.execCommand((Player) passenger, nearestGate.getCommand(), nearestGate.getCommandType());
					}
					if (passenger instanceof Player && nearestGate.getMessage() != null) {
						((Player) passenger).sendMessage(nearestGate.getMessage());
					}

					if (passenger instanceof Player) {
						Plugin.lastMessageTime.put(((Player) passenger).getName(), now);
					}
				} else if (nearestGate.getBungeeTo() != null) {
					TeleportUtil.teleportVehicle(vehicle, nearestGate.getBungeeTo(), nearestGate.getBungeeType(), nearestGate.getTeleportEntities(), nearestGate.getTeleportInventory(), from.getBlockY() == to.getBlockY(), nearestGate.getCommand(), nearestGate.getCommandType(),
					        nearestGate.getMessage());
				} else if (passenger instanceof Player) {
					ExecuteUtil.execCommand(vehicle, nearestGate.getCommand(), nearestGate.getCommandType(), true);
					if (passenger instanceof Player) {
						Plugin.lastMessageTime.put(((Player) passenger).getName(), now);
					}
				}
			}
		}
	}

}
