package com.hyperlynx.lost_found;

import com.hyperlynx.lost_found.capabilities.IWorldInventory;
import com.hyperlynx.lost_found.capabilities.WorldInventory;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class LostFishedCommand {
    private static final SimpleCommandExceptionType ERROR_EMPTY = new SimpleCommandExceptionType(new LiteralMessage("The lost item queue is empty!"));
    private static final SimpleCommandExceptionType ERROR_WI_NOT_PRESENT = new SimpleCommandExceptionType(new LiteralMessage("The queue couldn't be found! This is a mod error."));


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command_builder = Commands.literal("lost-fished")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("peek")
                    .executes(LostFishedCommand::peekQueue))
                .then(Commands.literal("retrieve")
                        .executes(LostFishedCommand::pop))
                .then(Commands.literal("discard")
                        .then(Commands.literal("top")
                                .executes(LostFishedCommand::popAndDelete))
                        .then(Commands.literal("all")
                                .executes(LostFishedCommand::reset)));

        dispatcher.register(command_builder);
    }

    private static int peekQueue(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        IWorldInventory queue = context.getSource().getLevel().getCapability(WorldInventory.INSTANCE).resolve().orElseThrow(ERROR_WI_NOT_PRESENT::create);
        ItemStack top = queue.peekItem();
        if(top == null)
            throw ERROR_EMPTY.create();
        context.getSource().sendSuccess(new TranslatableComponent(top.getDescriptionId()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int pop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        IWorldInventory queue = context.getSource().getLevel().getCapability(WorldInventory.INSTANCE).resolve().orElseThrow(ERROR_WI_NOT_PRESENT::create);
        ItemStack stack = queue.popItem();
        if(stack == null)
            throw ERROR_EMPTY.create();
        Vec3 drop_point = context.getSource().getPosition();
        ItemEntity drop = new ItemEntity(context.getSource().getLevel(), drop_point.x, drop_point.y, drop_point.z, stack);
        context.getSource().getLevel().addFreshEntity(drop);
        return Command.SINGLE_SUCCESS;
    }

    private static int popAndDelete(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        IWorldInventory queue = context.getSource().getLevel().getCapability(WorldInventory.INSTANCE).resolve().orElseThrow(ERROR_WI_NOT_PRESENT::create);
        queue.popItem();
        return Command.SINGLE_SUCCESS;
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        IWorldInventory queue = context.getSource().getLevel().getCapability(WorldInventory.INSTANCE).resolve().orElseThrow(ERROR_WI_NOT_PRESENT::create);
        queue.reset();
        return Command.SINGLE_SUCCESS;
    }
}
