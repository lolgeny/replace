package com.lolad.femtoblaze.core.mixin

import com.lolad.femtoblaze.game_event.duck.MinecraftServerDuck
import net.minecraft.entity.Entity
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.server.function.CommandFunction
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.event.GameEvent
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import com.lolad.femtoblaze.game_event.GameEvent as GameEventModule

@Mixin(ServerWorld::class)
abstract class ServerWorldMixin {
    @Inject(
        method = ["emitGameEvent(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/event/GameEvent;Lnet/minecraft/util/math/BlockPos;)V"],
        at = [At("TAIL")]
    )
    private fun emitGameEvent(entity: Entity?, event: GameEvent, pos: BlockPos, ci: CallbackInfo) {
        for ((_, game_event_function) in ((this as ServerWorld).server as MinecraftServerDuck).getGameEventManager()!!.gameEvents) {
            if (game_event_function.trigger.id == event.id) {
                for (predicate in game_event_function.predicate) {
                    val context = LootContext.Builder(this as ServerWorld)
                        .parameter(LootContextParameters.ORIGIN, Vec3d.of(pos))
                        .optionalParameter(LootContextParameters.THIS_ENTITY, entity)
                        .build(GameEventModule.EVENT_CONTEXT)
                    if (!predicate.test(context)) {
                        continue
                    }
                }
                server.commandFunctionManager.getFunction(game_event_function.function.id)
                    .ifPresent { command: CommandFunction? ->
                        var source = server.commandFunctionManager.taggedFunctionSource
                            .withPosition(Vec3d.of(pos))
                        if (entity != null) {
                            source = source.withEntity(entity)
                        }
                        server.commandFunctionManager.execute(
                            command,
                            source
                        )
                    }
            }
        }
    }

}