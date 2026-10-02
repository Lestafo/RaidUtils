package com.dinamyter.raidutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.neoforged.neoforge.common.NeoForgeConfig;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.HashSet;
import java.util.Set;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RaidUtils.MODID)
public class RaidUtils {

    public static final String MODID = "raidutils";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Set<Integer> trackedRaids = new HashSet<>();

    public RaidUtils(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);


        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.PLACEHOLDER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }



    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("raidutils")
                        .requires(executor -> executor.hasPermission(1))
                        .then(
                                Commands.literal("start")
                                        .then(
                                                Commands.argument("omenlvl", IntegerArgumentType.integer(0)
                                                        ).then(Commands.argument("position", Vec3Argument.vec3())
                                                        .executes(context -> startRaid(context.getSource(), IntegerArgumentType.getInteger(context, "omenlvl"), Vec3Argument.getCoordinates(context,"position"))))
                                        )

                        )
                        .then(Commands.literal("stop")
                                .then(Commands.argument("Raid_Id", IntegerArgumentType.integer())
                                .executes(context -> stopRaid(context.getSource(),IntegerArgumentType.getInteger(context,"Raid_Id"))))
                        )
                        .then(Commands.literal("query")
                                .executes(context->queryTrackedRaids(context.getSource())))


        );
    }

    private static int startRaid(CommandSourceStack source, int level, Coordinates location) throws CommandSyntaxException {
        ServerPlayer serverplayer = source.getPlayerOrException();
        BlockPos blockpos = serverplayer.blockPosition();
        Raids raids = source.getLevel().getRaids();
        Raid raid = raids.createOrExtendRaid(serverplayer, location.getBlockPos(source)); //TODO: Transform raids.java getOrCreateRaid to public to untie from player, or do a mixin
        if (raid != null) {
            raid.setRaidOmenLevel(level);
            raids.setDirty();
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                    "Started a raid of level " + raid.getRaidOmenLevel() + " And of Id: {" + raid.getId() + "}"
            ), false);
            trackedRaids.add(raid.getId());
            IEventBus eventBus = NeoForge.EVENT_BUS;
            eventBus.addListener(RaidUtils::onServerTick);
        } else {
            source.sendFailure(Component.literal("Failed to create a raid in your local village"));
        }

        return 1;

    }

    private static int queryTrackedRaids(CommandSourceStack source){ //TODO: known bug: If server closes while raids are open, loses track
        if (trackedRaids.isEmpty())
            return 0;
        Raids raids = source.getLevel().getRaids();
        for(int id : trackedRaids){
            source.sendSuccess(()-> net.minecraft.network.chat.Component.literal(
                    "Found raid with ID {" + id + "} at: " + raids.get(id).getCenter().toShortString()), true);
        }
        return 1;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {

            for (Integer id : trackedRaids) {
                Raid raid = level.getRaids().get(id);

                if (raid != null && raid.isVictory()) {
                        level.getServer().createCommandSourceStack().sendSystemMessage(Component.literal("Raid Vitoriosa"));

                    // Your code here

                    trackedRaids.remove(id);
                }
            }
        }
    }

    public void raidVictory(){

    }
    private static int stopRaid(CommandSourceStack source, int id) throws CommandSyntaxException {
        Raid raid = source.getLevel().getRaids().get(id);
        if (raid != null) {
            raid.stop();
            source.sendSuccess(() -> Component.literal("Stopped raid"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("No raid found with ID: " + id));
            return -1;
        }
    }


}
