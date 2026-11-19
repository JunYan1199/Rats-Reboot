from pathlib import Path
import bpy, math, struct, collections
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
from rig_export import collect_rig
from mathutils import Vector, Quaternion
root=Path(__file__).resolve().parent
out=root.parents[1]/'src/main/resources/assets/guildjane'
(out/'meshes').mkdir(parents=True,exist_ok=True)
for src in sorted((root/'source').glob('*/*.fbx')):
    key=src.parent.name
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.fbx(filepath=str(src))
    rig=next(o for o in bpy.context.scene.objects if o.type=='ARMATURE')
    bones=rig.pose.bones
    # Lower both arms in world coordinates while preserving the imported bind orientation.
    for side,sign in [('L',1),('R',-1)]:
        upper=bones['Bip001 '+side+' UpperArm']
        hand=bones['Bip001 '+side+' Hand']
        orig=(rig.matrix_world@(hand.head)-rig.matrix_world@(upper.head)).normalized()
        target=Vector((sign*.23,-.12,-.96)).normalized()
        bone_world=(rig.matrix_world @ upper.matrix).to_quaternion()
        local=bone_world.inverted() @ orig.rotation_difference(target) @ bone_world
        upper.rotation_mode='QUATERNION'
        upper.rotation_quaternion=upper.rotation_quaternion @ local
        bpy.context.view_layer.update()
    tail=[bones[f'Skn_Tail_{i:02}'] for i in range(1,10)]
    positions=[rig.matrix_world@b.head for b in tail]+[rig.matrix_world@bones['Skn_TailJian_01'].head]
    next_pos=positions[0].copy()
    for i,(bone,heading,pitch) in enumerate(zip(tail,[5,10,22,42,69,95,120,145,155],[-24,-48,-64,-63,-46,-21,12,42,61])):
        yaw,tilt=math.radians(heading),math.radians(pitch)
        forward=Vector((math.sin(yaw)*math.cos(tilt),math.cos(yaw)*math.cos(tilt),math.sin(tilt)))
        delta=positions[i+1]-positions[i]
        world=rig.matrix_world@bone.matrix
        turn=delta.normalized().rotation_difference(forward).to_matrix().to_4x4()
        world=turn@world
        world.translation=next_pos
        bone.matrix=rig.matrix_world.inverted()@world
        next_pos=next_pos+delta.length*forward
    bpy.context.view_layer.update()
    streams=collections.defaultdict(list)
    weight_ids=collections.defaultdict(list)
    get_weights,save_rig=collect_rig(rig)
    for o in list(bpy.context.scene.objects):
        if o.type!='MESH': continue
        # Two held weapons, a separate shader shadow shell, and an untextured effect screen.
        if 'Weapon_1' in o.name or 'Weapon_2' in o.name or 'HairShadow' in o.name or 'Screen' in o.name:
            o.hide_render=True
            o.hide_set(True)
            continue
        ev=o.evaluated_get(bpy.context.evaluated_depsgraph_get()); m=ev.to_mesh()
        mat=ev.matrix_world; norm=mat.to_3x3().inverted().transposed()
        groups=collections.Counter()
        for v in o.data.vertices:
            if v.groups: groups[o.vertex_groups[max(v.groups,key=lambda g:g.weight).group].name]+=1
        print('PART',key,o.name,groups.most_common(4),flush=True)
        m.calc_loop_triangles()
        weights=get_weights(o)
        assert len(weights)==len(m.vertices)
        channels=m.uv_layers.active.data
        for tri in m.loop_triangles:
            name=m.materials[tri.material_index].name
            tex='face' if any(s in name for s in ('Face','Eye')) else 'body' if any(s in name for s in ('Body_1','Nocturne_Body')) else 'hair'
            for vi,li in zip(tri.vertices,tri.loops):
                weight_ids[tex].append(weights[vi])
                p=mat@m.vertices[vi].co; n=(norm@m.corner_normals[li].vector).normalized(); u,v=channels[li].uv
                streams[tex].append((p.x,p.z,p.y,u,1-v,n.x,n.z,n.y))
        ev.to_mesh_clear()
    allv=[v for values in streams.values() for v in values]
    ymin=min(v[1] for v in allv); ymax=max(v[1] for v in allv)
    scale=1.8/(ymax-ymin)
    save_rig(out/'meshes'/f'{key}.rig',weight_ids,ymin,scale)
    with (out/'meshes'/f'{key}.bin').open('wb') as g:
        g.write(struct.pack('>i',len(streams)))
        for tex,vs in streams.items():
            ident=(key+'_'+tex).encode()
            g.write(struct.pack('>H',len(ident))+ident+struct.pack('>i',len(vs)))
            for x,y,z,u,v,nx,ny,nz in vs: g.write(struct.pack('>8f',x*scale,(y-ymin)*scale,z*scale,u,v,nx,ny,nz))
    print('BOUNDS',key, [(round(min(p[i] for p in allv)*scale,3),round(max(p[i] for p in allv)*scale,3)) for i in range(3)],flush=True)

