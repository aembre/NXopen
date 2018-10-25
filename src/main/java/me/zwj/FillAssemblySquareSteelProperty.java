package me.zwj;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nxopen.AttributePropertiesBuilder;
import nxopen.Body;
import nxopen.BodyCollection;
import nxopen.Edge;
import nxopen.Face;
import nxopen.ListingWindow;
import nxopen.NXException;
import nxopen.NXObject;
import nxopen.Part;
import nxopen.Session;
import nxopen.SessionFactory;
import nxopen.TaggedObjectCollection.Iterator;
import nxopen.UFSession;
import nxopen.UI;
import nxopen.assemblies.Component;
import nxopen.assemblies.ComponentAssembly;
import nxopen.uf.UFModeling;
import nxopen.uf.UFModeling.AskVectorAngleData;
import nxopen.uf.UFVec3;

/**
 * 装配体方钢属性快捷填写
 * @ClassName: ComponentTest 
 * @author 张文杰
 * @date 2018年10月24日 上午10:02:14 
 *
 */
public class FillAssemblySquareSteelProperty {

	public static void main(String[] args) throws Exception {
		Session session = (Session)SessionFactory.get("Session");
		UFSession ufSession = (UFSession) SessionFactory.get("UFSession");
		UI theUI = (UI) SessionFactory.get("UI");
		Part workPart = session.parts().work();
		if(workPart == null){
			theUI.nxmessageBox().show("温馨提示",
					nxopen.NXMessageBox.DialogType.WARNING, "请先打开装配图");
			return;
		}
		UFModeling ufmodl = ufSession.modeling();
		UFVec3 vec3 = ufSession.vec3();
		ListingWindow lw = session.listingWindow();;
		lw.open();
		//组件装配
		ComponentAssembly componentAssembly = workPart.componentAssembly();
		//根组件
		Component rootComponent = componentAssembly.rootComponent();
		if(rootComponent == null){
			theUI.nxmessageBox().show("温馨提示",
					nxopen.NXMessageBox.DialogType.WARNING, "请确认打开的为装配图");
			return;
		}
		//所有底层组件
		List<Component> bottomComList = getAllBottomComponents(rootComponent);
		for (Component component : bottomComList) {
			//设置组件为非关闭状态
			component.unsuppress();
			//组件的原型为part
			Part part = (Part) component.prototype();
			//设置组件完全加载
			if(!part.isFullyLoaded()){
				part.loadFully();
			}
			//获取组件中所有方钢实体
			List<Body> bodyList = new ArrayList<>();
			//是否为贯穿方钢
			boolean isPenetrationSquareSteel = false;
			BodyCollection bodyCollection = part.bodies();
			Iterator bodyIterator = bodyCollection.iterator();
			while(bodyIterator.hasNext()){
				Body body = (Body) bodyIterator.next();
				if(body.isSolidBody() && isSquareSteel(body)){
					bodyList.add(body);
					isPenetrationSquareSteel = isPenetrationSquareSteel(body) ? true : false; 
				}
			}
			if(bodyList.size() == 1){
				Body body = bodyList.get(0);
				//底面集合
				List<Face> bottomFaceList = new ArrayList<>();
				//侧面集合
				List<Face> slideFaceList = new ArrayList<>();
				//所有顶面尺寸
				Set<Double> topFaceLengthSet = new HashSet<>();
				Face[] faces = body.getFaces();
				for (Face face : faces) {
					int num = 0;
					for (Face face2 : faces) {
						if(face != face2){
							double[] d1 = ufmodl.askFaceData(face.tag()).dir;
							double[] d2 = ufmodl.askFaceData(face2.tag()).dir;
							//0不平行，1平行
							int isParallel = vec3.isParallel(d1, d2, 0.0001);
							if(isParallel == 1){
								num++;
							}
						}
					}
					//贯穿方钢底面跟其他面平行的个数只能为0或1
					if(isPenetrationSquareSteel){
						if(num == 0 || num == 1){
							bottomFaceList.add(face);
						}else{
							slideFaceList.add(face);
						}
					}else{
						//非贯穿方钢底面跟其他面平行的个数只能为1或2
						if(num == 1 || num == 2){
							bottomFaceList.add(face);
						}else{
							slideFaceList.add(face);
						}
					}
					Edge[] edges = face.getEdges();
					if(edges.length == 8){
						for (Edge edge : edges) {
							topFaceLengthSet.add(Math.rint(edge.getLength()));
						}
					}
				}
				//长宽厚信息
				Map<String, Double> lengthMap = new HashMap<>();
				if(isPenetrationSquareSteel){
					//取与侧面垂直的底面数据，贯穿方钢只有一个
					for (Face face : bottomFaceList) {
						int num = 0;
						for (Face slideFace : slideFaceList) {
							double[] d1 = ufmodl.askFaceData(face.tag()).dir;
							double[] d2 = ufmodl.askFaceData(slideFace.tag()).dir;
							AskVectorAngleData angle = ufmodl.askVectorAngle(d1,d2);
					    	if(angle.smallAngle>1.570796){//判断顶面与侧面是否垂直
					    		num++;
					    	}
						}
						if(num == 8){
							Edge[] edges = face.getEdges();
							Set<Double> bottomEdgeLenSet = new HashSet<>();
							for (Edge edge : edges) {
								double length = Math.rint(edge.getLength());
								bottomEdgeLenSet.add(length);
							}
							List<Double> bottomEdgeLenList = new ArrayList<>(bottomEdgeLenSet);
							Collections.sort(bottomEdgeLenList);
							//可能性1：长宽相等
							if(bottomEdgeLenList.size() == 2){
								Double bodyLength = bottomEdgeLenList.get(1);
								Double bodyWidth = bottomEdgeLenList.get(1);
								lengthMap.put("bodyLength", bodyLength);
								lengthMap.put("bodyWidth", bodyWidth);
								lengthMap.put("bodyThickness", (bodyLength - bottomEdgeLenList.get(0))/2);
							}else if(bottomEdgeLenList.size() == 3){
								//可能性2：有一个相等
								Double bodyLength = bottomEdgeLenList.get(2);
								Double bodyWidth = bottomEdgeLenList.get(1);
								lengthMap.put("bodyLength", bodyLength);
								lengthMap.put("bodyWidth", bodyWidth);
								lengthMap.put("bodyThickness", (bodyLength - bodyWidth)/2);
							}else if(bottomEdgeLenList.size() == 4){
								//可能性3：各不相等
								Double value0 = bottomEdgeLenList.get(0);
								Double value1 = bottomEdgeLenList.get(1);
								Double value2 = bottomEdgeLenList.get(2);
								Double value3 = bottomEdgeLenList.get(3);
								if((value3 - value2) == (value1 - value0)){
									lengthMap.put("bodyLength", value3);
									lengthMap.put("bodyWidth", value1);
									lengthMap.put("bodyThickness", (value3 - value2)/2);
								}else{
									lengthMap.put("bodyLength", value3);
									lengthMap.put("bodyWidth", value2);
									lengthMap.put("bodyThickness", (value3 - value1)/2);
								}
							}
							break;
						}
					}
				}else{
					//取与侧面垂直的底面数据，非贯穿有两个
					for (Face face : bottomFaceList) {
						Edge[] edges = face.getEdges();
						if(edges.length == 8){
							continue;
						}
						double faceLength = 0;
						double faceWidth = 0;
						for (Edge edge : edges) {
							double length = Math.rint(edge.getLength());
							if(length >= faceLength){
								faceLength = length;
							}else{
								faceWidth = length;
							}
						}
						if(faceWidth == 0){
							faceWidth = faceLength;
						}
						if(!lengthMap.containsKey("bodyLength")){
							lengthMap.put("bodyLength", faceLength);
						}else{
							lengthMap.put("bodyThickness", Math.abs((faceLength - lengthMap.get("bodyLength"))/2));
							if(faceLength > lengthMap.get("bodyLength")){
								lengthMap.put("bodyLength", faceLength);
							}
						}
						if(!lengthMap.containsKey("bodyWidth")){
							lengthMap.put("bodyWidth", faceWidth);
						}else{
							if(faceWidth > lengthMap.get("bodyWidth")){
								lengthMap.put("bodyWidth", faceWidth);
							}
						}
					}
				}
				//方钢高度
				//所有侧面尺寸
				Set<Double> slideFaceLengthSet = new HashSet<>();
				for (Face face : slideFaceList) {
					Edge[] edges = face.getEdges();
					for (Edge edge : edges) {
						double length = Math.rint(edge.getLength());
						slideFaceLengthSet.add(length);
					}
				}
				slideFaceLengthSet.removeAll(topFaceLengthSet);
				List<Double> heightList = new ArrayList<>(slideFaceLengthSet);
				//heightList中的最大值即为方钢高度
				Collections.sort(heightList);
				Double height = heightList.get(heightList.size()-1);
				//写入到对应属性
			    NXObject[] nxObjects = new NXObject[]{component};
			    //装配体组件属性
			    AttributePropertiesBuilder propertiesBuilder = session.attributeManager().createAttributePropertiesBuilder(part, nxObjects, nxopen.AttributePropertiesBuilder.OperationType.NONE);
			    propertiesBuilder.setTitle("材料");
			    String materialAttrValue = lengthMap.get("bodyLength").intValue() + "*" + lengthMap.get("bodyWidth").intValue() + "*" + lengthMap.get("bodyThickness");
			    propertiesBuilder.setStringValue(materialAttrValue);
			    propertiesBuilder.createAttribute();
			    propertiesBuilder.setTitle("备注");
			    String remarkAttrValue = height.intValue()+"mm";
			    propertiesBuilder.setStringValue(remarkAttrValue);
			    propertiesBuilder.createAttribute();
			    propertiesBuilder.commit();
			    //组件文件属性
			    nxObjects = new NXObject[]{part};
			    AttributePropertiesBuilder partPropertiesBuilder = session.attributeManager().createAttributePropertiesBuilder(part, nxObjects, nxopen.AttributePropertiesBuilder.OperationType.NONE);
			    partPropertiesBuilder.setTitle("材料");
			    partPropertiesBuilder.setStringValue(materialAttrValue);
			    partPropertiesBuilder.createAttribute();
			    partPropertiesBuilder.setTitle("备注");
			    partPropertiesBuilder.setStringValue(remarkAttrValue);
			    partPropertiesBuilder.createAttribute();
			    partPropertiesBuilder.commit();
			    lw.writeLine("*组件\"" + component.displayName() + "\"已自动计算并填写正确");
			    lw.writeLine("   材料：" + materialAttrValue);
			    lw.writeLine("   备注：" + remarkAttrValue);
			}else if(bodyList.size() > 1){
				lw.writeLine("*组件\"" + component.displayName() + "\"的方钢模型不唯一，请处理后再计算或手动填写属性");
			}else{
				lw.writeLine("*组件\"" + component.displayName() + "\"无方钢模型");
			}
		}
	}

	/**
	 * 递归获取所有最底层的组件
	 * @param rootComponent
	 * @return 
	 * @return
	 * @throws NXException 
	 * @throws RemoteException 
	 */
	private static List<Component> getAllBottomComponents(Component rootComponent) throws Exception{
		List<Component> bottomComs = new ArrayList<>();
		addChildrenComponents(rootComponent, bottomComs);
		return bottomComs;
	}
	
	private static void addChildrenComponents(Component component, List<Component> comList) throws Exception{
		Component[] children = component.getChildren();
		for (Component com : children) {
			if(com.getChildren().length == 0){
				comList.add(com);
			}else{
				addChildrenComponents(com, comList);
			}
		}
	}
	
	/**
	 * 判断实体是否为方钢
	 * 方钢种类：1、贯穿方钢。2、抽壳方钢
	 * @param body
	 * @return
	 * @throws Exception 
	 * @throws  
	 */
	private static boolean isSquareSteel(Body body) throws Exception{
		Edge[] edges = body.getEdges();
		Face[] faces = body.getFaces();
		//暂时根据面和边的数量判断
		if(24!=edges.length || 11!=faces.length && 10!=faces.length){
			return false;
		}
		return true;
	}
	
	/**
	 * 是否为贯穿方钢
	 * @param body
	 * @return
	 * @throws Exception
	 */
	private static boolean isPenetrationSquareSteel(Body body) throws Exception{
		Edge[] edges = body.getEdges();
		Face[] faces = body.getFaces();
		//暂时根据面和边的数量判断
		if(24!=edges.length || 10!=faces.length){
			return false;
		}
		return true;
	}

}
