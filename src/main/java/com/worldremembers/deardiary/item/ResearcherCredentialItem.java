package com.worldremembers.deardiary.item;

import com.worldremembers.deardiary.network.DearDiaryNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class ResearcherCredentialItem extends Item {
    public ResearcherCredentialItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            DearDiaryNetworking.openResearcherCredential(serverPlayer);
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}
