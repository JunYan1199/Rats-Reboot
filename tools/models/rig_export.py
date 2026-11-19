import collections,struct
from mathutils import Vector

# Reduction keeps all FBX weights, merging helpers driven by the same animated parent.
def collect_rig(rig):
    labels={'Bip001 Pelvis':'hips','Bip001 Spine':'waist','Bip001 Spine2':'chest','Bip001 Neck':'neck','Bip001 Head':'head','Skn_M_Mouth':'mouth'}
    for side in ['L','R']:
        for source,target in [('Clavicle','collar'),('UpperArm','arm'),('Forearm','elbow'),('Hand','hand'),('Thigh','thigh'),('Calf','knee'),('Foot','foot'),('Toe0','toe')]:
            labels[f'Bip001 {side} {source}']=side+target
        labels[f'Ctr_{side}_Ear_01']=side+'ear'
        labels[f'Ctr_{side}_EarHair_01']=side+'earhair'
        labels[f'Skn_{side}_Mouth']=side+'mouth'
    for i in range(1,10):labels[f'Skn_Tail_{i:02}']='tail'+str(i)
    bones=[b for b in rig.pose.bones if b.name in labels]
    bones.sort(key=lambda b:len(b.parent_recursive) if 'Tail_' not in b.name else 100+int(b.name[-2:]))
    indices={b.name:i for i,b in enumerate(bones)}
    def closest(b):
        while b and b.name not in indices:b=b.parent
        return indices[b.name] if b else -1
    rigdata=[]
    for b in bones:
        parent=closest(b.parent)
        if b.name.startswith('Skn_Tail_') and int(b.name[-2:])>1:parent=indices[f'Skn_Tail_{int(b.name[-2:])-1:02}']
        rigdata.append((labels[b.name],parent,rig.matrix_world@b.head))
    table={}; usage=collections.Counter()
    def weights(obj):
        group_index=[closest(rig.pose.bones.get(g.name)) for g in obj.vertex_groups]
        ret=[]
        for v in obj.data.vertices:
            row=collections.Counter()
            for g in v.groups:
                j=group_index[g.group]
                if j>=0 and g.weight>0:row[j]+=g.weight
            if not row:row[indices['Bip001 Pelvis']]=1
            total=sum(row.values())
            signature=tuple((k,round(val/total,5)) for k,val in sorted(row.items()) if val/total>.00001)
            if signature not in table:table[signature]=len(table)
            for k,wt in signature:usage[rigdata[k][0]]+=wt
            ret.append(table[signature])
        return ret
    def save(path,indices_per_part,ymin,scale):
        with path.open('wb') as f:
            f.write(b'GJR1'+struct.pack('>i',len(rigdata)))
            for label,parent,p in rigdata:
                n=label.encode();f.write(struct.pack('>H',len(n))+n+struct.pack('>i3f',parent,-p.x*scale,(p.z-ymin)*scale,p.y*scale))
            f.write(struct.pack('>i',len(table)))
            for row in table:
                f.write(struct.pack('>B',len(row)))
                for j,wt in row:f.write(struct.pack('>Bf',j,wt/sum(v for _,v in row)))
            f.write(struct.pack('>i',len(indices_per_part)))
            for ids in indices_per_part.values():
                f.write(struct.pack('>i',len(ids)));f.write(struct.pack('>'+str(len(ids))+'H',*ids))
        print('RIG',path.name,len(rigdata),len(table),dict((s,round(v)) for s,v in usage.items() if 'ear' in s or 'mouth' in s),flush=True)
    return weights,save

