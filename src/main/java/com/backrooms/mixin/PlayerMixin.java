package com.backrooms.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class PlayerMixin {

    @Inject(method = "takeKnockback", at = @At("HEAD"), cancellable = true)
    private void reflectKnockback(double strength, double x, double z, CallbackInfo ci) {
        LivingEntity player = (LivingEntity) (Object) this;

        // Count how many pieces of Rubber Armor the player is wearing
        int rubberArmorCount = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack armorPiece = player.getEquippedStack(slot);
                LoreComponent lore = armorPiece.get(DataComponentTypes.LORE);
                if (lore != null) {
                    List<Text> loreLines = lore.lines();
                    if (!loreLines.isEmpty()) {
                        if (loreLines.getFirst().getString().equals("Rubber")) {
                            rubberArmorCount++;
                        }
                    }
                }
            }
        }

        if (rubberArmorCount > 0) {
            ci.cancel(); // Disable knockback for the player

            // Calculate how much knockback to return to the attacker
            double reflectedStrength = strength * (rubberArmorCount * 0.25); // 25% per piece

            if (rubberArmorCount < 4) {
                double strength2 = strength * (1 - (rubberArmorCount * 0.25));
                Vec3d knockbackVec = new Vec3d(x, 0, z).normalize().multiply(strength2);
                _takeKnockback(player, strength2, knockbackVec.x, knockbackVec.z);
            }

            if (player.getAttacker() instanceof LivingEntity attacker) {
                Vec3d knockbackVec2 = new Vec3d(x, 0, z).normalize().multiply(reflectedStrength);
                _takeKnockback(attacker, reflectedStrength, -knockbackVec2.x, -knockbackVec2.z);
            }
        }
    }

    private void _takeKnockback(LivingEntity player, double strength, double x, double z) {
        strength *= 1.0 - player.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE);
        if (!(strength <= 0.0)) {
            player.velocityDirty = true;

            Vec3d vec3d;
            for(vec3d = player.getVelocity(); x * x + z * z < 9.999999747378752E-6; z = (Math.random() - Math.random()) * 0.01) {
                x = (Math.random() - Math.random()) * 0.01;
            }

            Vec3d vec3d2 = (new Vec3d(x, 0.0, z)).normalize().multiply(strength);
            player.setVelocity(vec3d.x / 2.0 - vec3d2.x, player.isOnGround() ? Math.min(0.4, vec3d.y / 2.0 + strength) : vec3d.y, vec3d.z / 2.0 - vec3d2.z);
        }
    }
}
