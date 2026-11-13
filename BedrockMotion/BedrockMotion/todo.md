horses
iron golem
villager
slime

items, toolbelt, attacking animations 

fix config keys and save and quit doesnt work ??









bedrockmotion (v0.1.6)
Render thread9.72%

    net.minecraft.client.renderer.entity.EntityRenderer.handler$zza000$bedrockmotion$extract()7.94%
        dev.bedrockmotion.client.runtime.BmRuntime.extract()7.94%
            dev.bedrockmotion.bedrock.EntityAnimator.update()3.49%
                dev.bedrockmotion.bedrock.EntityAnimator.runController()2.15%
                    dev.bedrockmotion.bedrock.EntityAnimator.runControllerInner()2.14%
                        dev.bedrockmotion.bedrock.EntityAnimator.playState()1.51%
                            dev.bedrockmotion.bedrock.EntityAnimator.runController()0.72%
                            dev.bedrockmotion.bedrock.EntityAnimator.play()0.68%
                                dev.bedrockmotion.bedrock.AnimationDef$Channel.sample()0.33%
                                    dev.bedrockmotion.molang.Script.runNum()0.13%
                                        dev.bedrockmotion.molang.Script.run()0.12%
                                            dev.bedrockmotion.molang.Script.exec()0.12%
                                                dev.bedrockmotion.molang.Nodes$Arith.eval()0.11%
                                                    dev.bedrockmotion.molang.Nodes$Arith.num()0.11%
                                                        dev.bedrockmotion.molang.MathFns$M1.num()0.04%
                                                        dev.bedrockmotion.molang.Nodes$Arith.num()0.03%
                                                        dev.bedrockmotion.molang.Expr.num()0.02%
                                                dev.bedrockmotion.molang.Nodes$QueryCall.eval()0.01%
                                    dev.bedrockmotion.bedrock.AnimationDef$Channel.eval()0.01%
                                dev.bedrockmotion.bedrock.Pose.get()0.15%
                                dev.bedrockmotion.molang.Script.runNum()0.03%
                            dev.bedrockmotion.molang.Script.runNum()0.04%
                            dev.bedrockmotion.bedrock.EntityAnimator.resolve()0.01%
                        dev.bedrockmotion.molang.Script.run()0.62%
                            dev.bedrockmotion.molang.Script.exec()0.62%
                                dev.bedrockmotion.molang.Nodes$And.eval()0.43%
                                    dev.bedrockmotion.molang.Nodes$And.eval()0.18%
                                    dev.bedrockmotion.molang.Nodes$Not.eval()0.08%
                                    dev.bedrockmotion.molang.Nodes$Arith.eval()0.07%
                                    dev.bedrockmotion.molang.Nodes$QueryCall.eval()0.05%
                                    dev.bedrockmotion.molang.Nodes$Or.eval()0.03%
                                dev.bedrockmotion.molang.Nodes$Arith.eval()0.07%
                                dev.bedrockmotion.molang.Nodes$QueryCall.eval()0.06%
                                dev.bedrockmotion.molang.Nodes$Or.eval()0.02%
                                dev.bedrockmotion.molang.Nodes$Not.eval()0.02%
                dev.bedrockmotion.molang.Script.runAll()0.50%
                dev.bedrockmotion.bedrock.EntityAnimator.seedEngineVars()0.22%
                dev.bedrockmotion.bedrock.EntityAnimator.play()0.16%
                dev.bedrockmotion.molang.Script.run()0.13%
                dev.bedrockmotion.bedrock.Pose.reset()0.07%
                java.util.HashMap$ValueIterator.next()0.01%
                dev.bedrockmotion.bedrock.EntityAnimator.resolve()0.01%
            dev.bedrockmotion.client.runtime.BmRuntime.fill()2.17%
                dev.bedrockmotion.client.runtime.BmRuntime.fillMobStates()0.46%
                    dev.bedrockmotion.client.runtime.BmReflect.flag()0.32%
                        dev.bedrockmotion.client.runtime.BmReflect.call()0.32%
                            dev.bedrockmotion.client.runtime.BmReflect.invoke()0.10%
                                java.lang.reflect.Method.invoke()0.10%
                                    jdk.internal.reflect.DirectMethodHandleAccessor.invoke()0.10%
                                        jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl()0.10%
                                            java.lang.invoke.Invokers$Holder.invokeExact_MT()0.10%
                                                java.lang.invoke.LambdaForm$MH/0x0000000098cba400.invoke()0.03%
                                                java.lang.invoke.LambdaForm$MH/0x0000000098cba000.invoke()0.02%
                                                java.lang.invoke.LambdaForm$MH/0x0000000098cba800.invoke()0.02%
                                                java.lang.invoke.LambdaForm$MH/0x0000000098e52c00.invoke()0.00%
                    dev.bedrockmotion.client.runtime.BmReflect.firstFlag()0.08%
                    java.util.HashMap.put()0.03%
                    dev.bedrockmotion.client.runtime.BmReflect.numberFloat()0.02%
                    dev.bedrockmotion.client.runtime.BmReflect.number()0.02%
                dev.bedrockmotion.client.runtime.BmRuntime.fillSpecialStates()0.37%
                    dev.bedrockmotion.client.runtime.BmKinetic.read()0.09%
                    dev.bedrockmotion.client.runtime.BmReflect.number()0.08%
                    dev.bedrockmotion.client.runtime.BmReflect.flag()0.06%
                    dev.bedrockmotion.client.runtime.BmRuntime.hasEffect()0.04%
                    java.util.HashMap.put()0.04%
                    dev.bedrockmotion.client.runtime.BmReflect.call()0.02%
                    java.util.HashMap.getOrDefault()0.01%
                dev.bedrockmotion.client.runtime.BmReflect.flag()0.37%
                    dev.bedrockmotion.client.runtime.BmReflect.call()0.37%
                        dev.bedrockmotion.client.runtime.BmReflect.invoke()0.21%
                            java.lang.reflect.Method.invoke()0.21%
                                jdk.internal.reflect.DirectMethodHandleAccessor.invoke()0.21%
                                    jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl()0.21%
                                        java.lang.invoke.Invokers$Holder.invokeExact_MT()0.21%
                                            java.lang.invoke.LambdaForm$MH/0x0000000098cb9c00.invoke()0.06%
                                                java.lang.invoke.LambdaForm$DMH/0x000000009720c400.invokeVirtual()0.06%
                                                    net.minecraft.world.entity.LivingEntity.onClimbable()0.06%
                                                        net.minecraft.core.TypedInstance.is()0.01%
                                                        net.minecraft.world.entity.Entity.getInBlockState()0.01%
                                            java.lang.invoke.LambdaForm$MH/0x0000000098cbb000.invoke()0.03%
                                            java.lang.invoke.LambdaForm$MH/0x0000000098cbac00.invoke()0.03%
                                            java.lang.invoke.LambdaForm$MH/0x0000000098c5f000.invoke()0.02%
                dev.bedrockmotion.client.runtime.BmMobStates.fill()0.24%
                    dev.bedrockmotion.client.runtime.BmReflect.numberFloat()0.06%
                    dev.bedrockmotion.client.runtime.BmReflect.flag()0.04%
                    dev.bedrockmotion.client.runtime.BmReflect.callWith()0.04%
                    dev.bedrockmotion.client.runtime.BmReflect.number()0.02%
                    dev.bedrockmotion.client.runtime.BmReflect.fieldNumber()0.02%
                    dev.bedrockmotion.client.runtime.BmMobStates.fuseTime()0.01%
                    java.util.HashMap.put()0.01%
                    dev.bedrockmotion.client.runtime.BmMobStates.indexOf()0.01%
                    dev.bedrockmotion.client.runtime.BmMobStates.panda()0.00%
                    dev.bedrockmotion.client.runtime.BmReflect.call()0.00%
                dev.bedrockmotion.client.runtime.BmRuntime.fillLivingStates()0.17%
                dev.bedrockmotion.client.runtime.BmReflect.numberFloat()0.12%
                dev.bedrockmotion.client.runtime.BmReflect.number()0.07%
                java.util.HashMap.clear()0.04%
                dev.bedrockmotion.molang.EntitySnapshot.resetEngineVars()0.04%
                dev.bedrockmotion.client.runtime.BmWalk.state()0.03%
                java.util.HashMap.put()0.02%
                net.minecraft.world.entity.AgeableMob.isBaby()0.02%
                net.minecraft.world.entity.Entity.isInLava()0.02%
                net.minecraft.world.entity.Entity.isOnFire()0.01%
            dev.bedrockmotion.client.runtime.BmModelPose.capture()1.37%
                dev.bedrockmotion.bedrock.PoseHierarchy.solve()0.78%
                    dev.bedrockmotion.bedrock.LocatorSolver.solveAll()0.57%
                        dev.bedrockmotion.bedrock.Pose.find()0.56%
                    dev.bedrockmotion.bedrock.PoseHierarchy.isIdentity()0.00%
                dev.bedrockmotion.bedrock.Pose.forEachTouched()0.48%
                    dev.bedrockmotion.client.runtime.BmModelPose$$Lambda/0x0000000098beb218.accept()0.36%
                        dev.bedrockmotion.client.runtime.BmModelPose.lambda$capture$0()0.36%
                            dev.bedrockmotion.bedrock.PoseHierarchy.normalize()0.15%
                            dev.bedrockmotion.bedrock.PoseHierarchy.indexOf()0.12%
                            java.util.HashSet.add()0.06%
                            java.util.ArrayList.add()0.02%
                    java.util.HashMap$EntryIterator.next()0.01%
                java.util.HashSet.contains()0.06%
                java.util.ArrayList.add()0.04%
            dev.bedrockmotion.bedrock.RenderPlanner.plan()0.36%
            dev.bedrockmotion.bedrock.Pose.touchedCount()0.14%
            dev.bedrockmotion.client.BedrockMotionClient.once()0.08%
            dev.bedrockmotion.client.render.BmFrame.build()0.04%
            dev.bedrockmotion.client.runtime.BmRuntime.frameGate()0.03%
            dev.bedrockmotion.client.runtime.BmRuntime.lookup()0.03%
            dev.bedrockmotion.client.runtime.BmRuntime.runParentSetups()0.02%
            net.minecraft.world.entity.Entity.isOnFire()0.02%
            dev.bedrockmotion.bedrock.EntityAnimator.<init>()0.00%
            java.util.ArrayList.clear()0.00%
    net.minecraft.client.renderer.feature.ModelFeatureRenderer.wrapOperation$zzc000$bedrockmotion$setupAnim()0.95%
        dev.bedrockmotion.client.runtime.BmModelHook.apply()0.84%
            dev.bedrockmotion.client.runtime.BmModelHook.drive()0.82%
                dev.bedrockmotion.client.runtime.BmModelHook.applyAncestors()0.32%
                dev.bedrockmotion.bedrock.PoseHierarchy.normalize()0.21%
                java.util.HashMap.get()0.09%
                java.util.ImmutableCollections$MapN.get()0.03%
                dev.bedrockmotion.client.runtime.BmModelHook.nestedInJava()0.03%
                java.util.Map.computeIfAbsent()0.01%
            java.util.Collections$SetFromMap.remove()0.01%
        net.minecraft.client.renderer.feature.ModelFeatureRenderer$$Lambda/0x0000000098c11000.call()0.11%
    net.minecraft.client.renderer.entity.EntityRenderDispatcher.wrapOperation$zzb000$bedrockmotion$submit()0.65%
    net.minecraft.client.renderer.item.ItemStackRenderState.handler$zze000$bedrockmotion$submit()0.13%
    net.minecraft.client.renderer.item.ItemModelResolver.handler$zzd000$bedrockmotion$recordItem()0.05%
    dev.bedrockmotion.client.render.BmFrame$$Lambda/0x0000000098e2f880.render()0.00%