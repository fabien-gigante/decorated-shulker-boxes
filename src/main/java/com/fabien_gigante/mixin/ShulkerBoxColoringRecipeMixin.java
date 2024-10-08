package com.fabien_gigante.mixin;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Overwrite;

import com.fabien_gigante.DecoratedShulkerBoxItemStack;

import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShulkerBoxColoringRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.DyeColor;
import net.minecraft.world.World;

@Mixin(ShulkerBoxColoringRecipe.class)
public class ShulkerBoxColoringRecipeMixin {

	// Helpers to search the recipe inventory for shulker box and dyes
	@Unique
	private static Stream<ItemStack> search(CraftingRecipeInput input, Predicate<ItemStack> condition) {
		return input.getStacks().stream().filter(condition);
	}
	@Unique
	private static final Predicate<ItemStack> isShulkerBox = (stack) -> Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock;
	@Unique
	private static final Predicate<ItemStack> isDye = (stack) -> stack.getItem() instanceof DyeItem;

	// Match recipes with exactly 1 shulker box and exactly 1 or 2 dyes
	/** @reason using overwrite because behavior change not easy by simple code injection @author fabien **/
	@Overwrite
	public boolean matches(CraftingRecipeInput input, World world) {
		long nShulkerBoxes = search(input, isShulkerBox).count();
		if (nShulkerBoxes != 1) return false;
		long nDyes = search(input, isDye).count();
		return (nDyes == 1 || nDyes == 2) && (input.getStackCount() == nShulkerBoxes + nDyes);
	}

	// Perform the dyed shulker box craft using the provided dyes and optional extra item
	/** @reason using overwrite because behavior change not easy by simple code injection @author fabien **/
	@Overwrite
	public ItemStack craft(CraftingRecipeInput input, WrapperLookup wrapperLookup) {
		ItemStack shulkerBox = search(input, isShulkerBox).findFirst().orElse(ItemStack.EMPTY);
		List<DyeColor> colors = search(input, isDye).map((stack) -> ((DyeItem) stack.getItem()).getColor()).toList();
		DyeColor primaryColor = colors.size() > 0 ? colors.get(0) : null;
		DyeColor secondaryColor = colors.size() > 1 ? colors.get(1) : null;

		// Reproduce vanilla behavior
		Block block = ShulkerBoxBlock.get(primaryColor);
		ItemStack dyedBox = shulkerBox.copyComponentsToNewStack(block, 1);

		// Additional behavior for secondary color and extra item
		DecoratedShulkerBoxItemStack.from(wrapperLookup, dyedBox).setSecondaryColor(secondaryColor != primaryColor ? secondaryColor : null);
		return dyedBox;
	}

}
