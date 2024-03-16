package code.patches;

import basemod.helpers.CardModifierManager;
import code.actions.WaitMoreAction;
import code.cardmods.IsSpecterModifier;
import code.cards.SpecterCard;
import code.vfx.PurpleNonmovingBlur;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import javassist.CannotCompileException;
import javassist.CtBehavior;

public class SpecterPatches {
    @SpirePatch(clz = GameActionManager.class, method = "getNextAction")
    public static class GhostSubvertPlay {
        @SpireInsertPatch(locator = Locator.class)
        public static void Insert(GameActionManager __instance) {
            CardQueueItem cqi = __instance.cardQueue.get(0);
            AbstractCard c = cqi.card;
            if (c != null) {
                if (CardModifierManager.hasModifier(c, IsSpecterModifier.ID)) {
                    if (__instance.cardQueue.get(0).monster == null) {
                        __instance.cardQueue.get(0).randomTarget = true;
                    }
                    AbstractDungeon.player.hand.removeCard(c);
                    IsSpecterModifier mod = (IsSpecterModifier) CardModifierManager.getModifiers(c, IsSpecterModifier.ID).get(0);
                    AbstractDungeon.player.cardInUse = mod.ghost;
                    __instance.cardQueue.get(0).card = mod.ghost;
                    mod.ghost.current_x = c.current_x;
                    mod.ghost.current_y = c.current_y;
                    mod.ghost.target_x = Settings.WIDTH / 2F;
                    mod.ghost.target_y = Settings.HEIGHT / 2F;

                    mod.ghost.dontTriggerOnUseCard = !cqi.autoplayCard && !mod.ghost.canUse(AbstractDungeon.player, null);


                    AbstractDungeon.actionManager.addToTop(new WaitMoreAction(0.25F));
                    AbstractDungeon.actionManager.addToTop(new AbstractGameAction() {
                        @Override
                        public void update() {
                            isDone = true;
                            CardCrawlGame.sound.play("ATTACK_WHIFF_2");
                            for (int itr = 0; itr < 50; itr++) {
                                AbstractDungeon.effectsQueue.add(new PurpleNonmovingBlur(mod.ghost.target_x, mod.ghost.target_y));
                            }
                        }
                    });
                }
            }
        }

        private static class Locator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.FieldAccessMatcher(GameActionManager.class, "usingCard");
                return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            }
        }
    }

    @SpirePatch(clz = GameActionManager.class, method = "getNextAction")
    public static class CantAffordDontUse {
        public static boolean SERIOUSLY_NO = false;

        @SpireInsertPatch(locator = Locator.class)
        public static void Insert(GameActionManager __instance) {
            AbstractCard c = __instance.cardQueue.get(0).card;
            if (c.cardID.equals(SpecterCard.ID) && c.dontTriggerOnUseCard) {
                SERIOUSLY_NO = true;
                AbstractDungeon.actionManager.addToBottom(new UseCardAction(c));
            }
        }

        private static class Locator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.MethodCallMatcher(AbstractPlayer.class, "useCard");
                return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            }
        }
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "useCard")
    public static class SeriouslyDontUseIt {
        @SpirePrefixPatch
        public static SpireReturn Prefix(AbstractPlayer __instance, AbstractCard c, AbstractMonster monster, int energyOnUse) {
            if (CantAffordDontUse.SERIOUSLY_NO) {
                CantAffordDontUse.SERIOUSLY_NO = false;
                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }
    }

    /* Decided not to copy over info, but in case
    @SpirePatch(clz = CardModifierManager.class, method = "addModifier")
    public static class CopyModifiers {
        public static void Prefix(AbstractCard card, AbstractCardModifier mod) {
            if (mod.shouldApply(card) && !(mod instanceof IsSpecterModifier) && !mod.isInherent(card)) {
                if (CardModifierManager.hasModifier(card, IsSpecterModifier.ID)) {
                    IsSpecterModifier ghostMod = (IsSpecterModifier) CardModifierManager.getModifiers(card, IsSpecterModifier.ID).get(0);
                    CardModifierManager.addModifier(ghostMod.ghost, mod.makeCopy());
                }
            }
        }
    }

     */
}