fix the rest of the animations
fix particles (warden death animation)

learn from polytone ?


fix iron golem
fix cape 
fix attack animations & item holding so its held in the arms where the animations are in general
fix idk horse
fix attached weapons
fix zombies walk animation (they look like they slide a bit on the ground bc of the mini steps if they do any) and when losing legs zombies crawling on the ground he firstly sometimes falls under the ground which shouldnt happen
ender dragon ??
warden ??



weiter
mod menu und cloth config integration beenden auch




optimize one day
bedrockmotion (v0.1.6)
Render thread7.34%

    net.minecraft.client.renderer.entity.EntityRenderer.handler$zza000$bedrockmotion$extract()5.70%
        dev.bedrockmotion.client.runtime.BmRuntime.extract()5.70%
            dev.bedrockmotion.bedrock.EntityAnimator.update()2.89%
                dev.bedrockmotion.bedrock.EntityAnimator.runController()1.71%
                    dev.bedrockmotion.bedrock.EntityAnimator.runControllerInner()1.70%
                        dev.bedrockmotion.bedrock.EntityAnimator.playState()1.31%
                            dev.bedrockmotion.bedrock.EntityAnimator.runController()0.54%
                                dev.bedrockmotion.bedrock.EntityAnimator.runControllerInner()0.54%
                                    dev.bedrockmotion.bedrock.EntityAnimator.playState()0.47%
                                        dev.bedrockmotion.bedrock.EntityAnimator.play()0.28%
                                            dev.bedrockmotion.bedrock.Pose.get()0.10%
                                                java.lang.String.toLowerCase()0.05%
                                                    java.lang.StringLatin1.toLowerCase()0.05%
                                                java.util.HashMap.computeIfAbsent()0.04%36ms (self: 4ms - 0.00%)
                                            dev.bedrockmotion.bedrock.AnimationDef$Channel.sample()0.06%
                                            dev.bedrockmotion.molang.Script.runNum()0.01%
                                        dev.bedrockmotion.bedrock.EntityAnimator.runController()0.11%
                                        java.lang.String.toLowerCase()0.02%
                                        java.util.LinkedHashMap.get()0.02%
                                        dev.bedrockmotion.bedrock.EntityAnimator.controllerFor()0.02%
                                        dev.bedrockmotion.bedrock.EntityAnimator.instance()0.02%
                                        dev.bedrockmotion.molang.Script.runNum()0.00%
                                    dev.bedrockmotion.molang.Script.run()0.06%
                            dev.bedrockmotion.bedrock.EntityAnimator.play()0.37%
                            java.lang.String.toLowerCase()0.16%
                            dev.bedrockmotion.bedrock.EntityAnimator.instance()0.10%
                            java.util.LinkedHashMap.get()0.06%
                            dev.bedrockmotion.molang.Script.runNum()0.03%
                            dev.bedrockmotion.bedrock.EntityAnimator.controllerFor()0.02%
                            dev.bedrockmotion.bedrock.BedrockPack.animation()0.01%
                        dev.bedrockmotion.molang.Script.run()0.37%
                        java.util.LinkedHashMap.get()0.00%
                dev.bedrockmotion.molang.Script.runAll()0.27%
                dev.bedrockmotion.bedrock.EntityAnimator.play()0.19%
                dev.bedrockmotion.bedrock.EntityAnimator.seedEngineVars()0.14%
                dev.bedrockmotion.bedrock.EntityAnimator.controllerFor()0.12%
                java.util.LinkedHashMap.get()0.12%
                java.lang.String.toLowerCase()0.10%
                dev.bedrockmotion.bedrock.Pose.reset()0.08%
                dev.bedrockmotion.molang.Script.run()0.06%
                dev.bedrockmotion.bedrock.BedrockPack.controller()0.01%
                java.util.HashMap$ValueIterator.next()0.00%
                dev.bedrockmotion.bedrock.BedrockPack.animation()0.00%
            dev.bedrockmotion.client.runtime.BmRuntime.fill()1.37%
                dev.bedrockmotion.client.runtime.BmReflect.flag()0.29%
                    dev.bedrockmotion.client.runtime.BmReflect.call()0.18%
                        dev.bedrockmotion.client.runtime.BmReflect.invoke()0.18%
                            java.lang.reflect.Method.invoke()0.18%
                                jdk.internal.reflect.DirectMethodHandleAccessor.invoke()0.18%
                                    jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl()0.18%
                                        java.lang.invoke.Invokers$Holder.invokeExact_MT()0.18%
                                            java.lang.invoke.LambdaForm$MH/0x000000000dc7bc00.invoke()0.09%
                                                java.lang.invoke.LambdaForm$DMH/0x000000000c20c400.invokeVirtual()0.09%
                                                    net.minecraft.world.entity.LivingEntity.onClimbable()0.09%
                                                        net.minecraft.core.TypedInstance.is()0.01%
                                                        net.minecraft.world.entity.Entity.getInBlockState()0.01%
                                            java.lang.invoke.LambdaForm$MH/0x000000000dc7b400.invoke()0.02%
                                            java.lang.invoke.LambdaForm$MH/0x000000000dc7b800.invoke()0.02%
                                            java.lang.invoke.LambdaForm$MH/0x000000000dcd5000.invoke()0.01%
                                            java.lang.invoke.LambdaForm$MH/0x000000000dc9ac00.invoke()0.00%
                                            java.lang.invoke.LambdaForm$MH/0x000000000dcd4800.invoke()0.00%
                dev.bedrockmotion.client.runtime.BmRuntime.fillMobStates()0.28%
                dev.bedrockmotion.client.runtime.BmRuntime.fillSpecialStates()0.28%
                dev.bedrockmotion.client.runtime.BmRuntime.fillLivingStates()0.14%
                dev.bedrockmotion.client.runtime.BmReflect.numberFloat()0.08%
                dev.bedrockmotion.molang.EntitySnapshot.resetEngineVars()0.06%
                dev.bedrockmotion.client.runtime.BmWalk.state()0.03%
                java.util.HashMap.put()0.02%
                java.util.HashMap.clear()0.02%
                net.minecraft.world.entity.LivingEntity.getMaxHealth()0.01%
                net.minecraft.world.entity.Entity.isInLava()0.01%
                net.minecraft.world.entity.Entity.isVehicle()0.00%
                net.minecraft.world.entity.LivingEntity.getItemBySlot()0.00%
            dev.bedrockmotion.client.runtime.BmModelPose.capture()0.90%
            dev.bedrockmotion.bedrock.RenderPlanner.plan()0.26%
            dev.bedrockmotion.client.BedrockMotionClient.once()0.04%
            dev.bedrockmotion.client.runtime.BmRuntime.lookup()0.02%
            dev.bedrockmotion.client.runtime.BmRuntime.frameGate()0.02%
            net.minecraft.world.entity.Entity.isOnFire()0.01%
            dev.bedrockmotion.client.render.BmFrame.replacesModel()0.00%
    net.minecraft.client.renderer.feature.ModelFeatureRenderer.wrapOperation$zzc000$bedrockmotion$setupAnim()0.88%
        dev.bedrockmotion.client.runtime.BmModelHook.apply()0.65%
            dev.bedrockmotion.client.runtime.BmModelHook.drive()0.65%
                dev.bedrockmotion.client.runtime.BmModelHook.applyAncestors()0.21%
                dev.bedrockmotion.bedrock.PoseHierarchy.normalize()0.17%
                java.util.HashMap.get()0.11%
                java.util.Map.computeIfAbsent()0.04%
                java.util.ImmutableCollections$MapN.get()0.02%
                dev.bedrockmotion.client.runtime.BmModelHook.nestedInJava()0.01%
        net.minecraft.client.renderer.feature.ModelFeatureRenderer$$Lambda/0x000000000dbe8c10.call()0.23%
    net.minecraft.client.renderer.entity.EntityRenderDispatcher.wrapOperation$zzb000$bedrockmotion$submit()0.76%
        net.minecraft.client.renderer.entity.EntityRenderDispatcher$$Lambda/0x000000000dc2f400.call()0.76%
            net.minecraft.client.renderer.entity.EntityRenderDispatcher.mixinextras$bridge$submit$17()0.76%
                net.minecraft.client.renderer.entity.AgeableMobRenderer.submit()0.42%
                net.minecraft.client.renderer.entity.LivingEntityRenderer.submit()0.18%
                net.minecraft.client.renderer.entity.ArmorStandRenderer.submit()0.05%
                net.minecraft.client.renderer.entity.SalmonRenderer.submit()0.03%
                net.minecraft.client.renderer.entity.ItemEntityRenderer.submit()0.02%
                net.minecraft.client.renderer.entity.CowRenderer.submit()0.02%
                net.minecraft.client.renderer.entity.PigRenderer.submit()0.02%
                net.minecraft.client.renderer.entity.EnderDragonRenderer.submit()0.01%
                net.minecraft.client.renderer.entity.ChickenRenderer.submit()0.00%