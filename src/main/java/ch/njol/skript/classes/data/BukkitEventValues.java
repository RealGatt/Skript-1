/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Copyright 2011-2017 Peter Güttinger and contributors
 */
package ch.njol.skript.classes.data;

import ch.njol.skript.Skript;
import ch.njol.skript.command.CommandEvent;
import ch.njol.skript.events.EvtMoveOn;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.*;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.vehicle.VehicleEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.weather.WeatherEvent;
import org.bukkit.event.world.ChunkEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Peter Güttinger
 */
@SuppressWarnings({"unchecked", "deprecation"})
public final class BukkitEventValues {
	
	public BukkitEventValues() {}
	
	protected static final boolean offHandSupport = Skript.isRunningMinecraft(1, 9);
	
	static {
		
		// === WorldEvents ===
		EventValues.registerEventValue(WorldEvent.class, World.class, new Getter<World, WorldEvent>() {
			@Override
			@Nullable
			public World get(final WorldEvent e) {
				return e.getWorld();
			}
		}, 0);
		// StructureGrowEvent - a WorldEvent
		EventValues.registerEventValue(StructureGrowEvent.class, Block.class, new Getter<Block, StructureGrowEvent>() {
			@Override
			@Nullable
			public Block get(final StructureGrowEvent e) {
				return e.getLocation().getBlock();
			}
		}, 0);
		EventValues.registerEventValue(StructureGrowEvent.class, Block.class, new Getter<Block, StructureGrowEvent>() {
			@Override
			@Nullable
			public Block get(final StructureGrowEvent e) {
				for (final BlockState bs : e.getBlocks()) {
					if (bs.getLocation().equals(e.getLocation()))
						return new BlockStateBlock(bs);
				}
				return e.getLocation().getBlock();
			}
		}, 1);
		// WeatherEvent - not a WorldEvent (wtf ô_Ô)
		EventValues.registerEventValue(WeatherEvent.class, World.class, new Getter<World, WeatherEvent>() {
			@Override
			@Nullable
			public World get(final WeatherEvent e) {
				return e.getWorld();
			}
		}, 0);
		// ChunkEvents
		EventValues.registerEventValue(ChunkEvent.class, Chunk.class, new Getter<Chunk, ChunkEvent>() {
			@Override
			@Nullable
			public Chunk get(final ChunkEvent e) {
				return e.getChunk();
			}
		}, 0);
		
		// === BlockEvents ===
		EventValues.registerEventValue(BlockEvent.class, Block.class, new Getter<Block, BlockEvent>() {
			@Override
			@Nullable
			public Block get(final BlockEvent e) {
				return e.getBlock();
			}
		}, 0);
		EventValues.registerEventValue(BlockEvent.class, World.class, new Getter<World, BlockEvent>() {
			@Override
			@Nullable
			public World get(final BlockEvent e) {
				return e.getBlock().getWorld();
			}
		}, 0);
		// REMIND workaround of the event's location being at the entity in block events that have an entity event value
		EventValues.registerEventValue(BlockEvent.class, Location.class, new Getter<Location, BlockEvent>() {
			@Override
			@Nullable
			public Location get(final BlockEvent e) {
				return BlockUtils.getLocation(e.getBlock());
			}
		}, 0);
		// BlockPlaceEvent
		EventValues.registerEventValue(BlockPlaceEvent.class, Player.class, new Getter<Player, BlockPlaceEvent>() {
			@Override
			@Nullable
			public Player get(final BlockPlaceEvent e) {
				return e.getPlayer();
			}
		}, 0);
		EventValues.registerEventValue(BlockPlaceEvent.class, Block.class, new Getter<Block, BlockPlaceEvent>() {
			@Override
			public Block get(final BlockPlaceEvent e) {
				return new BlockStateBlock(e.getBlockReplacedState());
			}
		}, -1);
		EventValues.registerEventValue(BlockPlaceEvent.class, Direction.class, new Getter<Direction, BlockPlaceEvent>() {
			@Override
			@Nullable
			public Direction get(final BlockPlaceEvent e) {
				if (e.getBlock() != null) {
					BlockFace bf = e.getBlockPlaced().getFace(e.getBlockAgainst());
					return new Direction(new double[]{bf.getModX(), bf.getModY(), bf.getModZ()});
				}
				return Direction.ZERO;
			}
		}, 0);
		// BlockFadeEvent
		EventValues.registerEventValue(BlockFadeEvent.class, Block.class, new Getter<Block, BlockFadeEvent>() {
			@Override
			@Nullable
			public Block get(final BlockFadeEvent e) {
				return e.getBlock();
			}
		}, -1);
		EventValues.registerEventValue(BlockFadeEvent.class, Block.class, new Getter<Block, BlockFadeEvent>() {
			@Override
			public Block get(final BlockFadeEvent e) {
				return new DelayedChangeBlock(e.getBlock(), e.getNewState());
			}
		}, 0);
		EventValues.registerEventValue(BlockFadeEvent.class, Block.class, new Getter<Block, BlockFadeEvent>() {
			@Override
			public Block get(final BlockFadeEvent e) {
				return new BlockStateBlock(e.getNewState());
			}
		}, 1);
		// BlockGrowEvent (+ BlockFormEvent)
		EventValues.registerEventValue(BlockGrowEvent.class, Block.class, new Getter<Block, BlockGrowEvent>() {
			@Override
			@Nullable
			public Block get(final BlockGrowEvent e) {
				if (e instanceof BlockSpreadEvent)
					return e.getBlock();
				return new BlockStateBlock(e.getNewState());
			}
		}, 0);
		EventValues.registerEventValue(BlockGrowEvent.class, Block.class, new Getter<Block, BlockGrowEvent>() {
			@Override
			@Nullable
			public Block get(final BlockGrowEvent e) {
				return e.getBlock();
			}
		}, -1);
		// BlockDamageEvent
		EventValues.registerEventValue(BlockDamageEvent.class, Player.class, new Getter<Player, BlockDamageEvent>() {
			@Override
			@Nullable
			public Player get(final BlockDamageEvent e) {
				return e.getPlayer();
			}
		}, 0);
		// BlockBreakEvent
		EventValues.registerEventValue(BlockBreakEvent.class, Player.class, new Getter<Player, BlockBreakEvent>() {
			@Override
			@Nullable
			public Player get(final BlockBreakEvent e) {
				return e.getPlayer();
			}
		}, 0);
		EventValues.registerEventValue(BlockBreakEvent.class, Block.class, new Getter<Block, BlockBreakEvent>() {
			@Override
			@Nullable
			public Block get(final BlockBreakEvent e) {
				return e.getBlock();
			}
		}, -1);
		EventValues.registerEventValue(BlockBreakEvent.class, Block.class, new Getter<Block, BlockBreakEvent>() {
			@Override
			public Block get(final BlockBreakEvent e) {
				return new DelayedChangeBlock(e.getBlock());
			}
		}, 0);
		EventValues.registerEventValue(BlockBreakEvent.class, Block.class, new Getter<Block, BlockBreakEvent>() {
			@Override
			public Block get(final BlockBreakEvent e) {
				final BlockState s = e.getBlock().getState();
				s.setType(s.getType() == Material.ICE ? Material.STATIONARY_WATER : Material.AIR);
				s.setRawData((byte) 0);
				return new BlockStateBlock(s, true);
			}
		}, 1);
		// BlockFromToEvent
		EventValues.registerEventValue(BlockFromToEvent.class, Block.class, new Getter<Block, BlockFromToEvent>() {
			@Override
			@Nullable
			public Block get(final BlockFromToEvent e) {
				return e.getToBlock();
			}
		}, 1);
		// BlockIgniteEvent
		EventValues.registerEventValue(BlockIgniteEvent.class, Player.class, new Getter<Player, BlockIgniteEvent>() {
			@Override
			@Nullable
			public Player get(final BlockIgniteEvent e) {
				return e.getPlayer();
			}
		}, 0);
		EventValues.registerEventValue(BlockIgniteEvent.class, Block.class, new Getter<Block, BlockIgniteEvent>() {
			@Override
			@Nullable
			public Block get(final BlockIgniteEvent e) {
				return e.getIgnitingBlock();
			}
		}, 0);
		// BlockDispenseEvent
		EventValues.registerEventValue(BlockDispenseEvent.class, ItemStack.class, new Getter<ItemStack, BlockDispenseEvent>() {
			@Override
			@Nullable
			public ItemStack get(final BlockDispenseEvent e) {
				return e.getItem();
			}
		}, 0);
		// BlockCanBuildEvent
		EventValues.registerEventValue(BlockCanBuildEvent.class, Block.class, new Getter<Block, BlockCanBuildEvent>() {
			@Override
			@Nullable
			public Block get(final BlockCanBuildEvent e) {
				return e.getBlock();
			}
		}, -1);
		EventValues.registerEventValue(BlockCanBuildEvent.class, Block.class, new Getter<Block, BlockCanBuildEvent>() {
			@Override
			public Block get(final BlockCanBuildEvent e) {
				final BlockState s = e.getBlock().getState();
				s.setTypeId(e.getMaterialId());
				s.setRawData((byte) 0);
				return new BlockStateBlock(s, true);
			}
		}, 0);
		// SignChangeEvent
		EventValues.registerEventValue(SignChangeEvent.class, Player.class, new Getter<Player, SignChangeEvent>() {
			@Override
			@Nullable
			public Player get(final SignChangeEvent e) {
				return e.getPlayer();
			}
		}, 0);
		
		// === EntityEvents ===
		EventValues.registerEventValue(EntityEvent.class, Entity.class, new Getter<Entity, EntityEvent>() {
			@Override
			@Nullable
			public Entity get(final EntityEvent e) {
				return e.getEntity();
			}
		}, 0, "Use 'attacker' and/or 'victim' in damage events", EntityDamageEvent.class);
		EventValues.registerEventValue(EntityEvent.class, World.class, new Getter<World, EntityEvent>() {
			@Override
			@Nullable
			public World get(final EntityEvent e) {
				return e.getEntity() == null ? null : e.getEntity().getWorld(); // no idea why it could be null, but it can happen
			}
		}, 0);
		// EntityDamageEvent
		EventValues.registerEventValue(EntityDamageEvent.class, DamageCause.class, new Getter<DamageCause, EntityDamageEvent>() {
			@Override
			@Nullable
			public DamageCause get(final EntityDamageEvent e) {
				return e.getCause();
			}
		}, 0);
		EventValues.registerEventValue(EntityDamageByEntityEvent.class, Projectile.class, new Getter<Projectile, EntityDamageByEntityEvent>() {
			@Override
			@Nullable
			public Projectile get(final EntityDamageByEntityEvent e) {
				if (e.getDamager() instanceof Projectile)
					return (Projectile) e.getDamager();
				return null;
			}
		}, 0);
		// EntityDeathEvent
		EventValues.registerEventValue(EntityDeathEvent.class, Projectile.class, new Getter<Projectile, EntityDeathEvent>() {
			@Override
			@Nullable
			public Projectile get(final EntityDeathEvent e) {
				final EntityDamageEvent ldc = e.getEntity().getLastDamageCause();
				if (ldc instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) ldc).getDamager() instanceof Projectile)
					return (Projectile) ((EntityDamageByEntityEvent) ldc).getDamager();
				return null;
			}
		}, 0);
		EventValues.registerEventValue(EntityDeathEvent.class, DamageCause.class, new Getter<DamageCause, EntityDeathEvent>() {
			@Override
			@Nullable
			public DamageCause get(final EntityDeathEvent e) {
				final EntityDamageEvent ldc = e.getEntity().getLastDamageCause();
				return ldc == null ? null : ldc.getCause();
			}
		}, 0);
		// ProjectileHitEvent
		EventValues.registerEventValue(ProjectileHitEvent.class, Entity.class, new Getter<Entity, ProjectileHitEvent>() {
			@Override
			@Nullable
			public Entity get(final ProjectileHitEvent e) {
				assert false;
				return e.getEntity();
			}
		}, 0, "Use 'projectile' and/or 'shooter' in projectile hit events", ProjectileHitEvent.class);
		EventValues.registerEventValue(ProjectileHitEvent.class, Projectile.class, new Getter<Projectile, ProjectileHitEvent>() {
			@Override
			@Nullable
			public Projectile get(final ProjectileHitEvent e) {
				return e.getEntity();
			}
		}, 0);
		// ProjectileLaunchEvent
		EventValues.registerEventValue(ProjectileLaunchEvent.class, Entity.class, new Getter<Entity, ProjectileLaunchEvent>() {
			@Override
			@Nullable
			public Entity get(final ProjectileLaunchEvent e) {
				assert false;
				return e.getEntity();
			}
		}, 0, "Use 'projectile' and/or 'shooter' in shoot events", ProjectileLaunchEvent.class);
		EventValues.registerEventValue(ProjectileLaunchEvent.class, Projectile.class, new Getter<Projectile, ProjectileLaunchEvent>() {
			@Override
			@Nullable
			public Projectile get(final ProjectileLaunchEvent e) {
				return e.getEntity();
			}
		}, 0);
		// EntityTameEvent
		EventValues.registerEventValue(EntityTameEvent.class, Entity.class, new Getter<Entity, EntityTameEvent>() {
			@Override
			@Nullable
			public Entity get(final EntityTameEvent e) {
				return e.getEntity();
			}
		}, 0);
		// EntityChangeBlockEvent
		EventValues.registerEventValue(EntityChangeBlockEvent.class, Block.class, new Getter<Block, EntityChangeBlockEvent>() {
			@Override
			@Nullable
			public Block get(final EntityChangeBlockEvent e) {
				return e.getBlock();
			}
		}, 0);
		// ItemSpawnEvent
		EventValues.registerEventValue(ItemSpawnEvent.class, ItemStack.class, new Getter<ItemStack, ItemSpawnEvent>() {
			@Override
			@Nullable
			public ItemStack get(final ItemSpawnEvent e) {
				return e.getEntity().getItemStack();
			}
		}, 0);
		
		// --- PlayerEvents ---
		EventValues.registerEventValue(PlayerEvent.class, Player.class, new Getter<Player, PlayerEvent>() {
			@Override
			@Nullable
			public Player get(final PlayerEvent e) {
				return e.getPlayer();
			}
		}, 0);
		EventValues.registerEventValue(PlayerEvent.class, World.class, new Getter<World, PlayerEvent>() {
			@Override
			@Nullable
			public World get(final PlayerEvent e) {
				return e.getPlayer().getWorld();
			}
		}, 0);
		// PlayerBedEnterEvent
		EventValues.registerEventValue(PlayerBedEnterEvent.class, Block.class, new Getter<Block, PlayerBedEnterEvent>() {
			@Override
			@Nullable
			public Block get(final PlayerBedEnterEvent e) {
				return e.getBed();
			}
		}, 0);
		// PlayerBedLeaveEvent
		EventValues.registerEventValue(PlayerBedLeaveEvent.class, Block.class, new Getter<Block, PlayerBedLeaveEvent>() {
			@Override
			@Nullable
			public Block get(final PlayerBedLeaveEvent e) {
				return e.getBed();
			}
		}, 0);
		// PlayerBucketEvents
		EventValues.registerEventValue(PlayerBucketFillEvent.class, Block.class, new Getter<Block, PlayerBucketFillEvent>() {
			@Override
			@Nullable
			public Block get(final PlayerBucketFillEvent e) {
				return e.getBlockClicked().getRelative(e.getBlockFace());
			}
		}, 0);
		EventValues.registerEventValue(PlayerBucketFillEvent.class, Block.class, new Getter<Block, PlayerBucketFillEvent>() {
			@Override
			@Nullable
			public Block get(final PlayerBucketFillEvent e) {
				final BlockState s = e.getBlockClicked().getRelative(e.getBlockFace()).getState();
				s.setTypeId(0);
				s.setRawData((byte) 0);
				return new BlockStateBlock(s, true);
			}
		}, 1);
		EventValues.registerEventValue(PlayerBucketEmptyEvent.class, Block.class, new Getter<Block, PlayerBucketEmptyEvent>() {
			@Override
			@Nullable
			public Block get(final PlayerBucketEmptyEvent e) {
				return e.getBlockClicked().getRelative(e.getBlockFace());
			}
		}, -1);
		EventValues.registerEventValue(PlayerBucketEmptyEvent.class, Block.class, new Getter<Block, PlayerBucketEmptyEvent>() {
			@Override
			public Block get(final PlayerBucketEmptyEvent e) {
				final BlockState s = e.getBlockClicked().getRelative(e.getBlockFace()).getState();
				s.setType(e.getBucket() == Material.WATER_BUCKET ? Material.STATIONARY_WATER : Material.STATIONARY_LAVA);
				s.setRawData((byte) 0);
				return new BlockStateBlock(s, true);
			}
		}, 0);
		// PlayerDropItemEvent
		EventValues.registerEventValue(PlayerDropItemEvent.class, Item.class, new Getter<Item, PlayerDropItemEvent>() {
			@Override
			@Nullable
			public Item get(final PlayerDropItemEvent e) {
				return e.getItemDrop();
			}
		}, 0);
		EventValues.registerEventValue(PlayerDropItemEvent.class, ItemStack.class, new Getter<ItemStack, PlayerDropItemEvent>() {
			@Override
			@Nullable
			public ItemStack get(final PlayerDropItemEvent e) {
				return e.getItemDrop().getItemStack();
			}
		}, 0);
		// PlayerPickupItemEvent
		EventValues.registerEventValue(PlayerPickupItemEvent.class, Item.class, new Getter<Item, PlayerPickupItemEvent>() {
			@Override
			@Nullable
			public Item get(final PlayerPickupItemEvent e) {
				return e.getItem();
			}
		}, 0);
		EventValues.registerEventValue(PlayerPickupItemEvent.class, ItemStack.class, new Getter<ItemStack, PlayerPickupItemEvent>() {
			@Override
			@Nullable
			public ItemStack get(final PlayerPickupItemEvent e) {
				return e.getItem().getItemStack();
			}
		}, 0);
		// PlayerItemConsumeEvent
		if (Skript.supports("org.bukkit.event.player.PlayerItemConsumeEvent")) {
			EventValues.registerEventValue(PlayerItemConsumeEvent.class, ItemStack.class, new Getter<ItemStack, PlayerItemConsumeEvent>() {
				@Override
				@Nullable
				public ItemStack get(final PlayerItemConsumeEvent e) {
					return e.getItem();
				}
			}, 0);
		}
		// PlayerItemBreakEvent
		if (Skript.supports("org.bukkit.event.player.PlayerItemBreakEvent")) {
			EventValues.registerEventValue(PlayerItemBreakEvent.class, ItemStack.class, new Getter<ItemStack, PlayerItemBreakEvent>() {
				@Override
				@Nullable
				public ItemStack get(final PlayerItemBreakEvent e) {
					return e.getBrokenItem();
				}
			}, 0);
		}
		// PlayerInteractEntityEvent
		EventValues.registerEventValue(PlayerInteractEntityEvent.class, Entity.class, new Getter<Entity, PlayerInteractEntityEvent>() {
			@Override
			@Nullable
			public Entity get(final PlayerInteractEntityEvent e) {
				return e.getRightClicked();
			}
		}, 0);
		EventValues.registerEventValue(PlayerInteractEntityEvent.class, ItemStack.class, new Getter<ItemStack, PlayerInteractEntityEvent>() {
			@Override
			@Nullable
			public ItemStack get(final PlayerInteractEntityEvent e) {
			return e.getPlayer().getItemInHand();
			}
		}, 0);
		// PlayerInteractEvent
		EventValues.registerEventValue(PlayerInteractEvent.class, Block.class, new Getter<Block, PlayerInteractEvent>() {
			@Override
			@Nullable
			public Block get(final PlayerInteractEvent e) {
				return e.getClickedBlock();
			}
		}, 0);
		EventValues.registerEventValue(PlayerInteractEvent.class, ItemStack.class, new Getter<ItemStack, PlayerInteractEvent>() {
			@Override
			@Nullable
			public ItemStack get(final PlayerInteractEvent e) {
				return e.getItem();
			}
		}, 0);
		EventValues.registerEventValue(PlayerInteractEvent.class, Direction.class, new Getter<Direction, PlayerInteractEvent>() {
			@Override
			@Nullable
			public Direction get(final PlayerInteractEvent e) {
				if (e.getBlockFace() != null)
					return new Direction(new double[]{e.getBlockFace().getModX(), e.getBlockFace().getModY(), e.getBlockFace().getModZ()});
				return Direction.ZERO; // Same as 'BlockFace.SELF' or literal 'at'
			}
		}, 0);
		// PlayerShearEntityEvent
		EventValues.registerEventValue(PlayerShearEntityEvent.class, Entity.class, new Getter<Entity, PlayerShearEntityEvent>() {
			@Override
			@Nullable
			public Entity get(final PlayerShearEntityEvent e) {
				return e.getEntity();
			}
		}, 0);
		// PlayerMoveEvent
		EventValues.registerEventValue(PlayerMoveEvent.class, Block.class, new Getter<Block, PlayerMoveEvent>() {
			@Override
			@Nullable
			public Block get(final PlayerMoveEvent e) {
				return EvtMoveOn.getBlock(e);
			}
		}, 0);
		
		// --- HangingEvents ---
		// 1.4.3
		if (Skript.classExists("org.bukkit.event.hanging.HangingEvent")) {
			EventValues.registerEventValue(HangingEvent.class, Hanging.class, new Getter<Hanging, HangingEvent>() {
				@Override
				@Nullable
				public Hanging get(final HangingEvent e) {
					return e.getEntity();
				}
			}, 0);
			EventValues.registerEventValue(HangingEvent.class, World.class, new Getter<World, HangingEvent>() {
				@Override
				@Nullable
				public World get(final HangingEvent e) {
					return e.getEntity().getWorld();
				}
			}, 0);
			EventValues.registerEventValue(HangingEvent.class, Location.class, new Getter<Location, HangingEvent>() {
				@Override
				@Nullable
				public Location get(final HangingEvent e) {
					return e.getEntity().getLocation();
				}
			}, 0);
			// HangingPlaceEvent
			EventValues.registerEventValue(HangingPlaceEvent.class, Player.class, new Getter<Player, HangingPlaceEvent>() {
				@Override
				@Nullable
				public Player get(final HangingPlaceEvent e) {
					return e.getPlayer();
				}
			}, 0);
		} // Finally removed old painting support - Spigot 1.9 removed the events
		
		// --- VehicleEvents ---
		EventValues.registerEventValue(VehicleEvent.class, Vehicle.class, new Getter<Vehicle, VehicleEvent>() {
			@Override
			@Nullable
			public Vehicle get(final VehicleEvent e) {
				return e.getVehicle();
			}
		}, 0);
		EventValues.registerEventValue(VehicleEvent.class, World.class, new Getter<World, VehicleEvent>() {
			@Override
			@Nullable
			public World get(final VehicleEvent e) {
				return e.getVehicle().getWorld();
			}
		}, 0);
		EventValues.registerEventValue(VehicleExitEvent.class, LivingEntity.class, new Getter<LivingEntity, VehicleExitEvent>() {
			@Override
			@Nullable
			public LivingEntity get(final VehicleExitEvent e) {
				return e.getExited();
			}
		}, 0);
		EventValues.registerEventValue(VehicleEvent.class, Entity.class, new Getter<Entity, VehicleEvent>() {
			@Override
			@Nullable
			public Entity get(final VehicleEvent e) {
				return e.getVehicle().getPassenger();
			}
		}, 0);
		
		// === CommandEvents ===
		// PlayerCommandPreprocessEvent is a PlayerEvent
		EventValues.registerEventValue(ServerCommandEvent.class, CommandSender.class, new Getter<CommandSender, ServerCommandEvent>() {
			@Override
			@Nullable
			public CommandSender get(final ServerCommandEvent e) {
				return e.getSender();
			}
		}, 0);
		EventValues.registerEventValue(CommandEvent.class, CommandSender.class, new Getter<CommandSender, CommandEvent>() {
			@Override
			public CommandSender get(final CommandEvent e) {
				return e.getSender();
			}
		}, 0);
		EventValues.registerEventValue(CommandEvent.class, World.class, new Getter<World, CommandEvent>() {
			@Override
			@Nullable
			public World get(final CommandEvent e) {
				return e.getSender() instanceof Player ? ((Player) e.getSender()).getWorld() : null;
			}
		}, 0);
		
		// === InventoryEvents ===
		// InventoryClickEvent
		EventValues.registerEventValue(InventoryClickEvent.class, Player.class, new Getter<Player, InventoryClickEvent>() {
			@Override
			@Nullable
			public Player get(final InventoryClickEvent e) {
				return e.getWhoClicked() instanceof Player ? (Player) e.getWhoClicked() : null;
			}
		}, 0);
		EventValues.registerEventValue(InventoryClickEvent.class, World.class, new Getter<World, InventoryClickEvent>() {
			@Override
			@Nullable
			public World get(final InventoryClickEvent e) {
				return e.getWhoClicked().getWorld();
			}
		}, 0);
		EventValues.registerEventValue(InventoryClickEvent.class, ItemStack.class, new Getter<ItemStack, InventoryClickEvent>() {
			@Override
			@Nullable
			public ItemStack get(final InventoryClickEvent e) {
				if (e instanceof CraftItemEvent)
					return ((CraftItemEvent) e).getRecipe().getResult();
				return e.getCurrentItem();
			}
		}, 0);
		EventValues.registerEventValue(InventoryClickEvent.class, Slot.class, new Getter<Slot, InventoryClickEvent>() {
			@SuppressWarnings("null")
			@Override
			@Nullable
			public Slot get(final InventoryClickEvent e) {
				return new InventorySlot(e.getInventory(), e.getSlot());
			}
		}, 0);
		EventValues.registerEventValue(InventoryClickEvent.class, InventoryAction.class, new Getter<InventoryAction, InventoryClickEvent>() {
			@Override
			@Nullable
			public InventoryAction get(final InventoryClickEvent e) {
				return e.getAction();
			}
		}, 0);
		EventValues.registerEventValue(InventoryClickEvent.class, ClickType.class, new Getter<ClickType, InventoryClickEvent>() {
			@Override
			@Nullable
			public ClickType get(final InventoryClickEvent e) {
				return e.getClick();
			}
		}, 0);
		// CraftItemEvent REMIND maybe re-add this when Skript parser is reworked?
//		EventValues.registerEventValue(CraftItemEvent.class, ItemStack.class, new Getter<ItemStack, CraftItemEvent>() {
//			@Override
//			@Nullable
//			public ItemStack get(final CraftItemEvent e) {
//				return e.getRecipe().getResult();
//			}
//		}, 0);
		// PrepareItemCraftEvent
		EventValues.registerEventValue(PrepareItemCraftEvent.class, Slot.class, new Getter<Slot, PrepareItemCraftEvent>() {
			@SuppressWarnings("null")
			@Override
			@Nullable
			public Slot get(final PrepareItemCraftEvent e) {
				return new InventorySlot(e.getInventory(), 9);
			}
		}, 0);
		EventValues.registerEventValue(PrepareItemCraftEvent.class, Player.class, new Getter<Player, PrepareItemCraftEvent>() {
			@Override
			@Nullable
			public Player get(final PrepareItemCraftEvent e) {
				List<HumanEntity> viewers = e.getInventory().getViewers(); // Get all viewers
				if (viewers.size() == 0) // ... if we don't have any
					return null;
				HumanEntity first = viewers.get(0); // Get first viewer and hope it is crafter
				if (first instanceof Player) // Needs to be player... Usually it is
					return (Player) first;
				return null;
			}
		}, 0);
		//InventoryOpenEvent
		EventValues.registerEventValue(InventoryOpenEvent.class, Player.class, new Getter<Player, InventoryOpenEvent>() {
			@Override
			@Nullable
			public Player get(final InventoryOpenEvent e) {
				return (Player) e.getPlayer();
			}
		}, 0);
		EventValues.registerEventValue(InventoryOpenEvent.class, Inventory.class, new Getter<Inventory, InventoryOpenEvent>() {
			@Override
			@Nullable
			public Inventory get(final InventoryOpenEvent e) {
				return e.getInventory();
			}
		}, 0);
		//InventoryCloseEvent
		EventValues.registerEventValue(InventoryCloseEvent.class, Player.class, new Getter<Player, InventoryCloseEvent>() {
			@Override
			@Nullable
			public Player get(final InventoryCloseEvent e) {
				return (Player) e.getPlayer();
			}
		}, 0);
		EventValues.registerEventValue(InventoryCloseEvent.class, Inventory.class, new Getter<Inventory, InventoryCloseEvent>() {
			@Override
			@Nullable
			public Inventory get(final InventoryCloseEvent e) {
				return e.getInventory();
			}
		}, 0);
		//PortalCreateEvent
		EventValues.registerEventValue(PortalCreateEvent.class, World.class, new Getter<World, PortalCreateEvent>() {
			@Override
			@Nullable
			public World get(final PortalCreateEvent e) {
				return e.getWorld();
			}
		}, 0);
		//PlayerEditBookEvent
		EventValues.registerEventValue(PlayerEditBookEvent.class, ItemStack.class, new Getter<ItemStack, PlayerEditBookEvent>() {
			@Override
			public ItemStack get(PlayerEditBookEvent e) {
				ItemStack book = new ItemStack(e.getPlayer().getItemInHand().getType());
				book.setItemMeta(e.getNewBookMeta());
				return book;
			}
		}, 0);
		
	}
}
