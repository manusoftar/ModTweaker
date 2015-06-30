package modtweaker2.mods.appeng.handlers;

import static modtweaker2.helpers.InputHelper.getStackDescription;
import static modtweaker2.helpers.InputHelper.toIItemStack;
import static modtweaker2.helpers.InputHelper.toStack;
import static modtweaker2.helpers.InputHelper.toStacks;
import static modtweaker2.helpers.StackHelper.matches;

import java.util.LinkedList;

import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import modtweaker2.helpers.InputHelper;
import modtweaker2.helpers.LogHelper;
import modtweaker2.mods.appeng.AppliedEnergisticsHelper;
import modtweaker2.utils.ArrayUtils;
import modtweaker2.utils.BaseListAddition;
import modtweaker2.utils.BaseListRemoval;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import appeng.api.AEApi;
import appeng.api.features.IInscriberRecipe;
import appeng.api.features.InscriberProcessType;
import appeng.core.features.registries.entries.InscriberRecipe;

@ZenClass("mods.appeng.Inscriber")
public class Inscriber {
    
    protected static final String name = "Applied Energistics 2 Inscriber";
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@ZenMethod
	public static void addRecipe(IItemStack[] imprintable, IItemStack plateA, IItemStack plateB, IItemStack out, String type) {
	    // Create recipe
	    IInscriberRecipe recipe = new InscriberRecipe(ArrayUtils.toArrayList(toStacks(imprintable)), toStack(out), toStack(plateA), toStack(plateB), InscriberProcessType.valueOf(type));
	    
        // Check if the recipe is already present, we don't want to add duplicates
	    for(IInscriberRecipe r : AEApi.instance().registries().inscriber().getRecipes()) {
	        if(r != null && AppliedEnergisticsHelper.equals(r, recipe)) {
	            LogHelper.logWarning(String.format("Duplicate %s Recipe found for %s. Command ignored!", Inscriber.name, InputHelper.getStackDescription(toStack(out))));
	            return;
	        }
	    }
	    
		MineTweakerAPI.apply(new Add(recipe));
	}

	public static class Add extends BaseListAddition<IInscriberRecipe> {
		public Add(IInscriberRecipe recipe) {
			super(Inscriber.name, AEApi.instance().registries().inscriber().getRecipes());
			recipes.add(recipe);
		}

		@Override
		public void apply() {
	        if(recipes.isEmpty()) {
	            return;
	        }

	        for(IInscriberRecipe recipe : recipes) {
	            try {
	                AEApi.instance().registries().inscriber().addRecipe(recipe);
	                successful.add(recipe);
	            } catch (Exception ex) { LogHelper.logError("Error adding inscriber recipe.", ex); }
	        }
		}
		
	    @Override
	    public void undo() {
	        if(successful.isEmpty()) {
	            return;
	        }

	        for(IInscriberRecipe recipe : successful) {
                try {
                    AEApi.instance().registries().inscriber().removeRecipe(recipe);
                } catch (Exception ex) { LogHelper.logError("Error removing inscriber recipe.", ex); }
	        }
	    }
	        
        @Override
        public String getRecipeInfo(IInscriberRecipe recipe) {
            return getStackDescription(recipe.getOutput());
        }
        
        @Override
        protected boolean equals(IInscriberRecipe recipe, IInscriberRecipe otherRecipe) {
            return AppliedEnergisticsHelper.equals(recipe, otherRecipe);
        }
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@ZenMethod
	public static void removeRecipe(IIngredient output) {
        // Get list of existing recipes, matching with parameter
        LinkedList<IInscriberRecipe> result = new LinkedList<IInscriberRecipe>();
        
        for(IInscriberRecipe entry : AEApi.instance().registries().inscriber().getRecipes()) {
            if(entry != null && entry.getOutput() != null && matches(output, toIItemStack(entry.getOutput()))) {
                result.add(entry);
            }
        }
        
        // Check if we found the recipes and apply the action
        if(!result.isEmpty()) {
            MineTweakerAPI.apply(new Remove(result));
        } else {
            LogHelper.logWarning(String.format("No %s Recipe found for %s. Command ignored!", Inscriber.name, output.toString()));
        }
	}

	public static class Remove extends BaseListRemoval<IInscriberRecipe> {
		public Remove(LinkedList<IInscriberRecipe> recipes) {
			super(Inscriber.name, AEApi.instance().registries().inscriber().getRecipes(), recipes);
		}
		
	    @Override
	    public void apply() {
	        if(this.recipes.isEmpty()) {
	            return;
	        }
	        
            for(IInscriberRecipe recipe : recipes) {
                try {
                    AEApi.instance().registries().inscriber().removeRecipe(recipe);
                    successful.add(recipe);
                } catch (Exception ex) { LogHelper.logError("Error removing inscriber recipe.", ex); }
            }
	    }
	    
	    @Override
	    public void undo() {
	        if(successful.isEmpty()) {
	            return;
	        }
	        
            for(IInscriberRecipe recipe : successful) {
                try {
                    AEApi.instance().registries().inscriber().addRecipe(recipe);
                } catch (Exception ex) { LogHelper.logError("Error restoring inscriber recipe.", ex); }
            }
	    }

	    @Override
		public String getRecipeInfo(IInscriberRecipe recipe) {
	        return getStackDescription(recipe.getOutput());
		}
	}
}
