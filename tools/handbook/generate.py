from pathlib import Path
import collections,json,re,sys,zipfile
from hashlib import sha256
here=Path(__file__).resolve().parent
resources=here.parents[1]/'src/main/resources'
class Resources:
 def namelist(self):return json.loads((here/'resource-order.json').read_text('utf8'))
 def read(self,name):return (resources/name).read_bytes()
z=Resources();files={};reg=json.loads((here/'registry.json').read_text('utf8'))
def ids(kind):return reg[kind]
items=set(ids('item'));blocks=set(ids('block'));entities=ids('entity_type')
lang={};fallback={}
for n in z.namelist():
 if re.fullmatch(r'assets/[^/]+/lang/(zh_cn|en_us).json',n):
  (lang if 'zh_cn' in n else fallback).update(json.loads(z.read(n)))
def label(kind,key):
 k=kind+'.'+key.replace(':','.');return lang.get(k,fallback.get(k))
def title(key):return label('item',key) or label('block',key) or key.split(':')[1].replace('_',' ')
def js(n,v):files[n]=json.dumps(v,ensure_ascii=False,indent=1).encode('utf8')
book='rats:field_manual';data='data/rats/patchouli_books/field_manual/';base='assets/rats/patchouli_books/field_manual/'
js(data+'book.json',dict(name='鼠鼠帝国手册',landing_text='从第一块奶酪开始，养出一支各有所长的鼠队。第一次养鼠，先翻「从这里开始」；遇到陌生升级、设备或生物，打开搜索输入名字。',subtitle='饲养 · 工坊 · 远征',version=1,use_resource_pack=True,show_progress=False))
js('data/rats/recipes/field_manual.json',dict(type='minecraft:crafting_shapeless',conditions=[dict(type='forge:mod_loaded',modid='patchouli')],ingredients=[{'item':'minecraft:book'},{'item':'rats:cheese'}],result={'item':'patchouli:guide_book','nbt':{'patchouli:book':book}}))
cats={};entries={};links={};refIcons={}
def cat(k,name,icon,desc):cats[k]=dict(name=name,icon=icon,description=desc,sortnum=len(cats))
def text(t):return dict(type='patchouli:text',text=t)
def entry(c,k,name,icon,pages):
 entryid=c+'/'+k;assert entryid not in entries
 entries[entryid]=dict(name=name,icon=icon,category='rats:'+c,pages=pages,sortnum=sum(i.startswith(c+'/') for i in entries));return entryid
def ref(k,name):return '$(l:'+k+')'+name+'$(/l)'
cat('start','从这里开始','minecraft:book','第一次驯鼠、第一项工作，以及想探索、经营或战斗时该翻哪一章。')
categories=[
 ('keeping','饲养、捕鼠与成长','rats:cheese','建家、驯养、喂食、繁育、基因和复活。','5949F8BA6D60D818'),
 ('work','升级与自动化','rats:rat_upgrade_basic','按工作类型找升级，搭建运输、采集和加工流程。','6D2FFD325ABCACF4'),
 ('island','鼠西洲远征','rats:token_fragment','从令牌出发，沿遗迹、材料和首领的路线探索鼠西洲。','5D2AFDE5BE4CE92C'),
 ('collection','装备、建筑与收藏','rats:rat_king_crown','穿戴、装饰、建筑和后期收集目标。','59AEA0CD525AA8FB'),
 ('evolution','进化与专精','rats:origin_stone','从 50 级进化到 100 级专精，逐一查看形态和能力。','6642F077A42E9811'),
 ('guild','公会与悬赏','guildjane:guild_deed','建起公会，为鼠队接单，等它们回来找简结账。','DAA829AEC0835FA6'),
 ('power','发电站与供能','guildjane:power_station','从三行配方到无线组网，查阅四种能源与手持存储装置。','C5D89A031F5F57FB')]
recipes=collections.defaultdict(list)
for n in z.namelist():
 m=re.fullmatch(r'data/(rats|ratlantis|guildjane)/recipes/(.*)\.json',n)
 if not m:continue
 r=json.loads(z.read(n));result=r.get('result',{});result=result.get('item') if isinstance(result,dict) else result
 if isinstance(result,str) and r.get('type') in ('minecraft:crafting_shaped','minecraft:crafting_shapeless') and not r.get('conditions'):recipes[result].append(m[1]+':'+m[2])
def recipe(icon):
 return [{'type':'patchouli:crafting','recipe':sorted(recipes[icon])[0]}] if recipes[icon] else []
def pages(lines):
 out=[];buf=''
 for ln in lines:
  ln=re.sub(r'&[0-9a-fklmnor]','',ln).strip()
  if not ln or ln.startswith(('{','【下一步】','读完点','读完勾','怎么看这张图','• 中间','• 主线','• 物品任务','这一组任务书','Ⅰ ','Ⅱ ','Ⅲ ','Ⅳ ')) or '点勾' in ln or '任务节点' in ln:continue
  # Keep individual pages readable at the standard book GUI scale.
  for seg in re.findall(r'.{1,76}(?:[。；，、！?？]|$)|.{1,76}',ln):
   if len(buf)+len(seg)>82:
    if buf:out.append(text(buf))
    buf=''
   buf+=('$(br2)' if buf else '')+seg
 if buf:out.append(text(buf))
 return out
for c,name,ico,desc,f in categories:
 cat(c,name,ico,desc)
 for row in json.loads((here/'chapters'/f'{f}.json').read_text('utf8')):
  ti=row.get('title','');icon=row.get('icon');icon=icon if isinstance(icon,str) and icon in items else ico
  if '从这里开始' in ti and c=='keeping':continue
  ps=pages(row.get('description',[]))
  if not ps:continue
  if c=='power' and '第一座' in ti:ps=pages(['工作台上排：铜块、遮光玻璃、铜块。','中排：红石块、下界合金锭、红石块。下排：钻石、遮光玻璃、钻石。','合成后准备长宽高各两格的空间。放下发电站，带来一只猪或自己驯养的电颊鼠。'])
  if icon!=ico or c in ('power','guild'):ps+=recipe(icon)
  k=entry(c,row['id'].lower(),re.sub(r'&[0-9a-fklmnor]','',ti),icon,ps)
  refIcons.setdefault(icon,k)
cat('brick','布瑞克的工作手册','rats:king_scepter','转化、下单做饭、装备职业帽和派工。')
def topic(c,k,n,i,ls):return entry(c,k,n,i,pages(ls)+recipe(i))
topic('brick','convert','从驯鼠到布瑞克','rats:brick_converter',['用布瑞克转化装置右键自己的驯服鼠，再用一次可以复原。转化后打开老鼠面板，给它准备职业需要的帽子和工具。','帽子决定职业；拿国王权杖右键自己的布瑞克，可以给当前职业派任务。'])
topic('brick','kitchen','开张第一间厨房','rats:brick_chef_hat',['给布瑞克戴厨师帽、准备锅铲。在点餐台附近放好配方所需的乐事工作方块和材料，打开点餐台选择菜品并下单。','用奶酪魔杖指定成品箱，先用材料齐全的一道菜验证出餐。没有出现预期菜品时，先检查对应烹饪模组、工作方块和食材。'])
for k,n,i,d in [('guard','侍卫','guard','装备武器和护甲，让侍卫跟随主人。遇到敌人时留出攻击距离；盾牌和武器的耐久也需要补充。'),('sentry','哨兵','sentry','在要守卫的地方设置驻守点，给哨兵准备剑。它会清理据点附近的敌人，把战斗经验结成经验瓶。'),('treasure','寻宝鼠','explorer','先给寻宝鼠设好家，让它在家附近寻宝。每日次数有限，主人进度会影响能找到的物资。'),('farmer','农夫','straw','准备锄头和作物，使用权杖设置田地的两个角，再观察第一轮耕作。'),('rancher','牧场主','cowboy','圈定牧场，准备适合动物的食物与工具，在职业界面设置繁殖上限。按需要安排喂养、剪毛或挤奶。')]:topic('brick',k,n+'上岗','rats:brick_hat_'+i,[d])
cat('magic','法术军团','rats:legion_baton','把自己的施法鼠编入同一小队并集中攻击。')
topic('magic','command','编队、分工与集火','rats:legion_baton',['手持指挥杖，潜行对空气右键切换职责，再右键自己的驯鼠编队。同一根杖对应同一队。','瞄准敌人右键发令，48 格内属于你的同队老鼠会收到目标。需要撤编时切换到自由模式，再右键原队员。','施法装备与对应模组联动。编好队后留意分配提示中的已装备数量，再试一次集火。'])
cat('sewer','披萨鼠与下水道','rats:pizza_slice','沿面包屑追查失物，在圆厅清剿鼠群并找回食物。')
topic('sewer','trail','追踪偷食鼠','rats:pizza_slice',['食物失窃后，沿面包屑寻找偷食鼠留下的鼠洞。右键入口进入这一次的下水道；中途可以从出口撤退，再从同一洞口回来。'])
topic('sewer','raid','圆厅的三波鼠群','rats:pizza_slice',['沿主通道向深处走到中段圆厅。简单或更高难度下，三波鼠群依次在圆厅两侧出现。清掉当前波次再准备下一波。','单人三波分别为 8、10、11 只；多人进入会有增援。支路可探索，主路直通尽头的大厅。'])
topic('sewer','boss','夺回失物','rats:pizza_slice',['第三波结束后到通道尽头找鼠头头。避开冲锋路线，等它撞墙眩晕再反击；披萨片落地后也要绕开。','击败它后拾取掉落，回入口寻找亮起的鼠洞。最后一位队员离开后，主世界的入口会塌成砂土。'])
cat('index','物品、方块与生物索引','minecraft:compass','按名称搜索，查物品说明、原料和对应玩法。设备的内部部件另列于设施页。')
loot=collections.defaultdict(list); dropfrom=collections.defaultdict(list); uses=collections.defaultdict(set)
def field_values(obj,field):
 if isinstance(obj,dict):
  if field in obj and isinstance(obj[field],str):yield obj[field]
  for v in obj.values():yield from field_values(v,field)
 elif isinstance(obj,list):
  for v in obj:yield from field_values(v,field)
for n in z.namelist():
 m=re.fullmatch(r'data/(rats|ratlantis|guildjane)/loot_tables/entities/(.+)\.json',n)
 if m:
  boss=m[1]+':'+m[2]
  for it in dict.fromkeys(field_values(json.loads(z.read(n)),'name')):
   if it in items or it.startswith('minecraft:'):
    loot[boss].append(it);dropfrom[it].append(boss)
 m=re.fullmatch(r'data/(rats|ratlantis|guildjane)/recipes/(.+)\.json',n)
 if m:
  r=json.loads(z.read(n));result=r.get('result');result=result.get('item') if isinstance(result,dict) else result
  if isinstance(result,str) and result in items:
   for key in ('key','ingredient','ingredients'):
    for ing in field_values(r.get(key),'item'):uses[ing].add(result)
for k,nam,ic in [('item','材料与工具','rats:cheese'),('block','设施与建筑方块','guildjane:power_station'),('entity','生物与投射物','minecraft:egg')]:
 cat('index_'+k,nam,ic,'搜索名称，查看登记名、现有物品提示和可查到的配方或掉落。');cats['index_'+k]['parent']='rats:index'
records=collections.defaultdict(list)
for key in sorted(items|blocks|set(entities)|{'guildjane:wireless_drive'}):
 group='entity' if key in entities and key not in items|blocks else 'block' if key in blocks or key.endswith(':wireless_drive') else 'item'
 records[group].append(key)
for group,keys in records.items():
 for j,key in enumerate(keys):
  name=(label('entity',key) if group=='entity' else title(key)) or key.split(':')[1]
  ic=key if key in items else key+'_spawn_egg' if key+'_spawn_egg' in items else 'minecraft:compass'
  desc=lang.get('item.'+key.replace(':','.')+'.desc') or fallback.get('item.'+key.replace(':','.')+'.desc')
  linksTo=refIcons.get(key)
  if key.endswith(':wireless_drive'):name='无线应力接收器';desc='用连接器选取发电站，再点击机械动力设备的空闲轴端，接收器会自动安装。'
  first={'type':'patchouli:spotlight','item':key,'text':desc or ''} if key in items else text(desc or '')
  first['text']+=(('$(br2)' if first['text'] else '')+('玩法：'+ref(linksTo,entries[linksTo]['name'])+'$(br2)' if linksTo else '')+'登记名：$(br)'+key)
  extra=[]
  if key in loot:extra+=pages(['战利品表列出的物品（实际掉落受条件和概率影响）：'+'、'.join(title(x) if x in items else x.split(':')[1] for x in loot[key])+'。'])
  if key in dropfrom:extra+=pages(['可在哪些生物的掉落表中找到：'+'、'.join(label('entity',x) or x for x in dict.fromkeys(dropfrom[key]))+'。'])
  if key in uses:extra+=pages(['可以参与制作：'+'、'.join(title(x) for x in sorted(uses[key])[:12])+'。'])
  entry('index_'+group,group+'_'+key.replace(':','_'),name,ic,[first]+recipe(key)+extra)
cat('milestones','冒险进度','minecraft:map','用游戏自带的进度目标检查你还没有尝试过的玩法。')
for n in z.namelist():
 m=re.fullmatch(r'data/(rats|ratlantis)/advancements/(?!recipes/)(.*)\.json',n)
 if not m:continue
 dis=json.loads(z.read(n)).get('display',{});t=dis.get('title',{});ds=dis.get('description',{})
 if not isinstance(t,dict) or not isinstance(ds,dict):continue
 title_=lang.get(t.get('translate'),fallback.get(t.get('translate')));d=lang.get(ds.get('translate'),fallback.get(ds.get('translate')))
 if title_ and d:entry('milestones',m[1]+'_'+m[2],title_,dis.get('icon',{}).get('item','minecraft:map'),pages([d]))
def first(c):return next(k for k in entries if k.startswith(c+'/'))
entry('start','welcome','第一天，从奶酪到第一只鼠','rats:cheese',[text('用一本书和一块奶酪可以再做一本手册。先在「饲养、捕鼠与成长」了解野鼠和驯服，再给自己的鼠安排第一项工作。'),text('$(li)'+ref(first('keeping'),'认识第一只鼠')+'$(li)'+ref(first('work'),'给老鼠装升级')+'$(li)'+ref(first('evolution'),'培养进化形态')) ,text('$(li)'+ref('brick/convert','经营布瑞克工坊')+'$(li)'+ref(first('guild'),'成立鼠鼠公会')+'$(li)'+ref(first('power'),'为设备供能')+'$(li)'+ref(first('island'),'远征鼠西洲')+'$(li)'+ref('sewer/trail','追查偷食鼠')+'$(li)'+ref('magic/command','指挥法术军团'))])
for typ,table in [('categories',cats),('entries',entries)]:
 for k,v in table.items():js(base+'en_us/'+typ+'/'+k+'.json',v)
# Patchouli uses en_us as its base content tree for all locales. This release's handbook is Chinese.
for k,v in entries.items():
 assert v['category'].split(':')[1] in cats
 for pg in v['pages']:
  for dest in re.findall(r'\$\(l:([^)]*)\)',pg.get('text','')):assert dest in entries,dest
  if 'recipe' in pg:
   ns,id_=pg['recipe'].split(':');assert f'data/{ns}/recipes/{id_}.json' in z.namelist()
for name,value in files.items():
 path=resources/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(value)
print(f'{len(entries)} entries, {len(cats)} categories')
