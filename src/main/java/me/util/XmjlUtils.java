package me.util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import nxopen.Body;
import nxopen.Curve;
import nxopen.DisplayableObject;
import nxopen.Line;
import nxopen.ListingWindow;
import nxopen.NXException;
import nxopen.Part;
import nxopen.PartLoadStatus;
import nxopen.Point;
import nxopen.Point3d;
import nxopen.Selection.MaskTriple;
import nxopen.Session;
import nxopen.SessionFactory;
import nxopen.Tag;
import nxopen.TaggedObject;
import nxopen.TaggedObjectCollection.Iterator;
import nxopen.TaggedObjectManager;
import nxopen.UFSession;
import nxopen.UI;
import nxopen.View;
import nxopen.annotations.Dimension;
import nxopen.annotations.LinearDimensionBuilder;
import nxopen.assemblies.Component;
import nxopen.blockstyler.PropertyList;
import nxopen.blockstyler.SelectObject;
import nxopen.drawings.DraftingBody;
import nxopen.drawings.DraftingCurve;
import nxopen.drawings.DraftingView;
import nxopen.uf.UFConstants;
import nxopen.uf.UFModeling;
import nxopen.uf.UFModlGeneral.AskCurveParmData;
import nxopen.uf.UFObj;
import nxopen.uf.UFObj.AskTypeAndSubtypeData;
import nxopen.uf.UFView.AskVisibleObjectsData;

public class XmjlUtils {
	private static Session theSession = null;
	private static UFSession ufSession = null;
	private static Part workPart = null;
	private static UI theUI = null;
	private static ListingWindow lw = null;

	static {
		try {
			theSession = (Session) SessionFactory.get("Session");
			ufSession = (UFSession) SessionFactory.get("UFSession");
			theUI = (UI) SessionFactory.get("UI");
			workPart = theSession.parts().work();
			lw = theSession.listingWindow();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取session
	 * 
	 * @return
	 */
	public static Session getSession() {
		return theSession;
	}

	/**
	 * 获取UFsession
	 * 
	 * @return
	 */
	public static UFSession getUfSession() {
		return ufSession;
	}

	/**
	 * 获取工作空间(workpart)
	 * 
	 * @return
	 */
	public static Part getWorkpart() {
		return workPart;
	}

	/**
	 * 获取listWindow提示框
	 * 
	 * @return
	 */
	public static ListingWindow getListWindow() {
		return lw;
	}

	/**
	 * 错误弹窗
	 * 
	 * @param message
	 *            弹窗信息
	 */
	public static void dialogError(String message) {
		try {
			theUI.nxmessageBox().show("Block Styler",
					nxopen.NXMessageBox.DialogType.ERROR, message);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 警告弹窗
	 * 
	 * @param message
	 */
	public static void dialogWarning(String message) {
		try {
			theUI.nxmessageBox().show("Block Styler",
					nxopen.NXMessageBox.DialogType.WARNING, message);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 问题弹窗
	 * 
	 * @param message
	 */
	public static void dialogQuestion(String message) {
		try {
			theUI.nxmessageBox().show("Block Styler",
					nxopen.NXMessageBox.DialogType.QUESTION, message);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 信息提示弹窗
	 * 
	 * @param message
	 */
	public static void dialogInformation(String message) {
		try {
			theUI.nxmessageBox().show("Block Styler",
					nxopen.NXMessageBox.DialogType.INFORMATION, message);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取所有非隐藏的实体
	 * 
	 * @return
	 * @throws Exception
	 */
	public static List<Body> getAllDisplayBody() {
		List<Body> list = null;// 存储实体对象
		try {
			list = new ArrayList<Body>();
			UFObj ufobj = ufSession.obj();
			UFModeling ufmodl = ufSession.modeling();
			TaggedObjectManager tagObjManager = theSession
					.taggedObjectManager();
			AskVisibleObjectsData askVisibleObjects = ufSession.view()
					.askVisibleObjects(Tag.NULL);
			Tag[] otTags = askVisibleObjects.visible;
			for (Tag tag : otTags) {
				AskTypeAndSubtypeData askTypeAndSubtype = ufobj
						.askTypeAndSubtype(tag);
				int type = askTypeAndSubtype.type;
				int subtype = askTypeAndSubtype.subtype;
				if (type == UFConstants.UF_solid_type
						&& subtype == UFConstants.UF_solid_body_subtype) {
					int bodyType = ufmodl.askBodyType(tag);
					if (bodyType == UFConstants.UF_MODL_SOLID_BODY) {
						Body body = (Body) tagObjManager.get(tag);
						list.add(body);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return list;
		}
		return list;
	}

	/**
	 * 设置工作空间为当前组件;
	 * 
	 * @param component
	 *            ;为null时代表默认工作空间(最顶层装配项)
	 */
	public static void setDisplayWorkpart(Component component) {
		try {
			PartLoadStatus partLoadStatus = theSession.parts()
					.setWorkComponent(component,
							nxopen.PartCollection.RefsetOption.ENTIRE,
							nxopen.PartCollection.WorkComponentOption.VISIBLE);
			workPart = theSession.parts().work();
			partLoadStatus.dispose();
			partLoadStatus = null;
		} catch (Exception e) {
			XmjlUtils.dialogError("此操作只能在工作空间！");
		}
	}

	/**
	 * 设置条件过滤(仅可用于过滤非实体元素)
	 * 
	 * @param selectObject
	 *            选择对象
	 * @param taggedObjects
	 *            可变参数，只选择给定的对象
	 */
	public static void setSelectionFilter(SelectObject selectObject,
			TaggedObject... taggedObjects) {
		try {
			List<MaskTriple> maskTripleList = new ArrayList<MaskTriple>();
			for (TaggedObject taggedObject : taggedObjects) {
				AskTypeAndSubtypeData ask = ufSession.obj().askTypeAndSubtype(
						taggedObject.tag());
				int type = ask.type;
				int subtype = ask.subtype;
				MaskTriple maskTriple = new MaskTriple(type, subtype, 0);
				maskTripleList.add(maskTriple);
			}
			MaskTriple[] maskTriples = (MaskTriple[]) maskTripleList.toArray();
			PropertyList properties = selectObject.getProperties();
			properties.setSelectionFilter("SelectionFilter",
					nxopen.Selection.SelectionAction.CLEAR_AND_ENABLE_SPECIFIC,
					maskTriples);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setSelectionFilter(SelectObject selection,
			MaskTriple[] maskTriples) {
		try {
			PropertyList properties = selection.getProperties();
			properties.setSelectionFilter("SelectionFilter",
					nxopen.Selection.SelectionAction.CLEAR_AND_ENABLE_SPECIFIC,
					maskTriples);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取对象的type和subtype
	 * 
	 * @param taggedObject
	 * @return
	 */
	public static AskTypeAndSubtypeData askTypeAndSubtype(
			TaggedObject taggedObject) {
		AskTypeAndSubtypeData ask = null;
		try {
			ask = ufSession.obj().askTypeAndSubtype(taggedObject.tag());
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
		return ask;
	}

	/**
	 * 获取曲线的起点坐标
	 * 
	 * @param curve
	 *            曲线
	 * @return
	 */
	public static Point3d getCurveStartPoint(Curve curve) {
		double[] point;
		Point3d point3d = null;
		try {
			point = ufSession.modlGeneral().askCurveProps(curve.tag(), 1).point;
			point3d = new Point3d(point[0], point[1], point[2]);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
		return point3d;
	}

	/**
	 * 获取曲线的起点坐标
	 * 
	 * @param curve
	 *            曲线
	 * @return
	 */
	public static Point3d getCurveStartPoint(DraftingCurve draftingCurve) {
		double[] point;
		Point3d point3d = null;
		try {
			point = ufSession.modlGeneral().askCurveProps(draftingCurve.tag(),
					1).point;
			point3d = new Point3d(point[0], point[1], point[2]);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
		return point3d;
	}

	/**
	 * 获取曲线的终点坐标
	 * 
	 * @param curve
	 *            曲线
	 * @return
	 */
	public static Point3d getCurveEndPoint(Curve curve) {
		double[] point;
		Point3d point3d = null;
		try {
			point = ufSession.modlGeneral().askCurveProps(curve.tag(), 0).point;
			point3d = new Point3d(point[0], point[1], point[2]);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
		return point3d;
	}

	/**
	 * 获取曲线的终点坐标
	 * 
	 * @param curve
	 *            曲线
	 * @return
	 */
	public static Point3d getCurveEndPoint(DraftingCurve draftingCurve) {
		double[] point;
		Point3d point3d = null;
		try {
			point = ufSession.modlGeneral().askCurveProps(draftingCurve.tag(),
					0).point;
			point3d = new Point3d(point[0], point[1], point[2]);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
		return point3d;
	}

	/**
	 * 获取曲线的中点坐标
	 * 
	 * @param curve
	 *            曲线
	 * @return
	 */
	public static Point3d getCurveCenterPoint(Curve curve) {
		double[] point;
		Point3d point3d = null;
		try {
			point = ufSession.modlGeneral().askCurveProps(curve.tag(), 0.5).point;
			point3d = new Point3d(point[0], point[1], point[2]);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
		return point3d;
	}

	/**
	 * 获取参考点在曲线上的映射点
	 * 
	 * @param curve
	 *            曲线
	 * @param referPoint
	 *            参考点
	 * @return
	 */
	public static Point3d getCurveOnePoint(Curve curve, double[] referPoint) {
		Point3d point3d = null;
		try {
			AskCurveParmData askCurveParm = ufSession.modlGeneral()
					.askCurveParm(curve.tag(), referPoint);
			double[] curvePnt = ufSession.modlGeneral().askCurveProps(
					curve.tag(), askCurveParm.parm).point;
			point3d = new Point3d(curvePnt[0], curvePnt[1], curvePnt[2]);
			/*
			 * double[] curvePnt = askCurveParm.curvePnt; point3d = new
			 * Point3d(curvePnt[0], curvePnt[1], curvePnt[2]);
			 */
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
		return point3d;
	}

	/**
	 * 以起点和终点自动标注给定直线
	 * 
	 * @param line
	 *            指定直线
	 */
	public static void createLinearDimension(Line line) {
		LinearDimensionBuilder builder;
		try {
			builder = workPart.dimensions().createLinearDimensionBuilder(null);

			builder.origin().setInferRelativeToGeometry(false);
			builder.origin()
					.setAnchor(
							nxopen.annotations.OriginBuilder.AlignmentPosition.TOP_CENTER);
			builder.origin()
					.plane()
					.setPlaneMethod(
							nxopen.annotations.PlaneBuilder.PlaneMethodType.XY_PLANE);

			builder.measurement().setDirection(null);
			builder.measurement().setDirectionView(null);

			builder.style()
					.dimensionStyle()
					.setNarrowDisplayType(
							nxopen.annotations.NarrowDisplayOption.NONE);

			// 起点
			nxopen.Point3d startPoint = getCurveStartPoint(line);
			nxopen.Point3d secondPoint = new nxopen.Point3d(0.0, 0.0, 0.0);
			builder.firstAssociativity().setValueWithSnap(
					nxopen.InferSnapType.SnapType.START, line,
					workPart.views().workView(), startPoint, null, null,
					secondPoint);
			// 终点
			nxopen.Point3d endPoint = getCurveEndPoint(line);
			builder.secondAssociativity().setValueWithSnap(
					nxopen.InferSnapType.SnapType.END, line,
					workPart.views().workView(), endPoint, null, null,
					secondPoint);
			// 标注点
			nxopen.Point3d valuePoint = getCurveCenterPoint(line);
			builder.origin().origin().setValue(null, null, valuePoint);

			builder.style().lineArrowStyle()
					.setLeaderOrientation(nxopen.annotations.LeaderSide.RIGHT);
			builder.style().dimensionStyle().setTextCentered(true);
			builder.commit();
			builder.destroy();

		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 根据给定的起点和终点标注直线
	 * 
	 * @param startPoint
	 *            起点
	 * @param endPoint
	 *            终点
	 */
	public static void createLinearDimension(Point3d startPoint,
			Point3d endPoint) {
		LinearDimensionBuilder builder;
		try {
			builder = workPart.dimensions().createLinearDimensionBuilder(null);

			builder.origin().setInferRelativeToGeometry(false);
			builder.origin()
					.setAnchor(
							nxopen.annotations.OriginBuilder.AlignmentPosition.TOP_CENTER);
			builder.origin()
					.plane()
					.setPlaneMethod(
							nxopen.annotations.PlaneBuilder.PlaneMethodType.XY_PLANE);

			builder.measurement().setDirection(null);
			builder.measurement().setDirectionView(null);

			builder.style()
					.dimensionStyle()
					.setNarrowDisplayType(
							nxopen.annotations.NarrowDisplayOption.NONE);

			Line line = workPart.curves().createLine(startPoint, endPoint);

			nxopen.Point3d secondPoint = new nxopen.Point3d(0.0, 0.0, 0.0);
			builder.firstAssociativity().setValueWithSnap(
					nxopen.InferSnapType.SnapType.START, line,
					workPart.views().workView(), startPoint, null, null,
					secondPoint);
			builder.secondAssociativity().setValueWithSnap(
					nxopen.InferSnapType.SnapType.END, line,
					workPart.views().workView(), endPoint, null, null,
					secondPoint);

			// 直线的终点，即标注点
			nxopen.Point3d valuePoint = getCurveCenterPoint(line);
			builder.origin().origin().setValue(null, null, valuePoint);

			builder.style().lineArrowStyle()
					.setLeaderOrientation(nxopen.annotations.LeaderSide.RIGHT);
			builder.style().dimensionStyle().setTextCentered(true);
			builder.commit();
			builder.destroy();
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 根据起点、终点、参考点标注直线尺寸
	 * 
	 * @param startPoint
	 *            起点
	 * @param endPoint
	 *            终点
	 * @param valuePoint
	 *            参考点
	 * @throws Exception 
	 */
	public static Dimension createLinearDimension(Point3d startPoint,
			Point3d endPoint, Point3d valuePoint, View view) throws Exception {
		LinearDimensionBuilder builder;
		Dimension dimension = null;
		try {
			builder = workPart.dimensions().createLinearDimensionBuilder(null);

			builder.origin().setInferRelativeToGeometry(false);
			builder.origin()
					.setAnchor(
							nxopen.annotations.OriginBuilder.AlignmentPosition.TOP_CENTER);
			builder.origin()
					.plane()
					.setPlaneMethod(
							nxopen.annotations.PlaneBuilder.PlaneMethodType.XY_PLANE);

			builder.measurement().setDirection(null);
			builder.measurement().setDirectionView(null);

			builder.style()
					.dimensionStyle()
					.setNarrowDisplayType(
							nxopen.annotations.NarrowDisplayOption.NONE);

//			Point3d mapStartPoint = mapModelToDrawing(view,startPoint);
//			Point3d mapEndPoint = mapModelToDrawing(view,endPoint);
			Line line = workPart.curves().createLine(startPoint, endPoint);
//			Line line1 = workPart.curves().createLine(mapStartPoint, mapEndPoint);
			hideObj(line);

			nxopen.Point3d secondPoint = new nxopen.Point3d(0.0, 0.0, 0.0);
			// nxopen.drawings.BaseView baseView1 =
			// ((nxopen.drawings.BaseView)workPart.draftingViews().findObject("Top@1"));
			//
			// builder.firstAssociativity().setValueWithSnap(nxopen.InferSnapType.SnapType.START,
			// line, baseView1, startPoint, null, null, secondPoint);
			// builder.secondAssociativity().setValueWithSnap(nxopen.InferSnapType.SnapType.END,
			// line, baseView1, endPoint, null, null, secondPoint);
			builder.firstAssociativity().setValueWithSnap(
					nxopen.InferSnapType.SnapType.START, line, view,
					startPoint, null, null, secondPoint);
			builder.secondAssociativity().setValueWithSnap(
					nxopen.InferSnapType.SnapType.END, line, view, endPoint,
					null, null, secondPoint);

			// 直线的终点，即标注点
			builder.origin().origin().setValue(null, null, valuePoint);

			builder.style().lineArrowStyle()
					.setLeaderOrientation(nxopen.annotations.LeaderSide.RIGHT);
			builder.style().dimensionStyle().setTextCentered(false);
			dimension = (Dimension) builder.commit();
			builder.destroy();
			
//			delete(line);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
			dialogInformation(e.toString());
		}
		return dimension;
	}

	public static void createLinearDimension(Point3d startPoint,
			Point3d endPoint, Point3d valuePoint,View view,
			DraftingCurve draftingCurve) throws Exception {
		LinearDimensionBuilder builder;
		try {
			builder = workPart.dimensions().createLinearDimensionBuilder(null);

			builder.origin().setInferRelativeToGeometry(false);
			builder.origin()
					.setAnchor(
							nxopen.annotations.OriginBuilder.AlignmentPosition.TOP_CENTER);
			builder.origin()
					.plane()
					.setPlaneMethod(
							nxopen.annotations.PlaneBuilder.PlaneMethodType.XY_PLANE);

			builder.measurement().setDirection(null);
			builder.measurement().setDirectionView(null);

			builder.style()
					.dimensionStyle()
					.setNarrowDisplayType(
							nxopen.annotations.NarrowDisplayOption.NONE);

//			Point3d startPoint = XmjlUtils.getCurveStartPoint(draftingCurve);
//			Point3d startPoint2 = mapModelToDrawing(view, startPoint);// 转换后的坐标
//			Point3d endPoint = XmjlUtils.getCurveEndPoint(draftingCurve);
//			Point3d endPoint2 = mapModelToDrawing(view, endPoint);// 转换后的坐标
//
//			Point3d valuePoint = new Point3d(endPoint2.x - 30,
//					(startPoint2.y + endPoint2.y) / 2, 0);
			/*if ("TOP".equals(position)) {
				valuePoint = new Point3d((startPoint2.x + endPoint2.x) / 2,
						(startPoint2.y + endPoint2.y) / 2 + length, 0);
			} else if ("BOTTOM".equals(position)) {
				valuePoint = new Point3d((startPoint2.x + endPoint2.x) / 2,
						(startPoint2.y + endPoint2.y) / 2 - length, 0);
			} else if ("LEFT".equals(position)) {
				valuePoint = new Point3d((startPoint2.x + endPoint2.x) / 2
						- length, (startPoint2.y + endPoint2.y) / 2, 0);
			} else if ("RIGHT".equals(position)) {
				valuePoint = new Point3d((startPoint2.x + endPoint2.x) / 2
						+ length, (startPoint2.y + endPoint2.y) / 2, 0);
			} else {
				valuePoint = null;
			}
*/
			nxopen.Point3d secondPoint = new nxopen.Point3d(0.0, 0.0, 0.0);

			builder.firstAssociativity().setValueWithSnap(
					nxopen.InferSnapType.SnapType.END, draftingCurve, view,
					startPoint, null, null, secondPoint);
			builder.secondAssociativity().setValueWithSnap(
					nxopen.InferSnapType.SnapType.START, draftingCurve, view,
					endPoint, null, null, secondPoint);

			// 直线的终点，即标注点
			builder.origin().origin().setValue(null, null, valuePoint);

			builder.style().lineArrowStyle()
					.setLeaderOrientation(nxopen.annotations.LeaderSide.RIGHT);
			builder.style().dimensionStyle().setTextCentered(true);
			builder.commit();
			builder.destroy();

		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 删除指定对象
	 * 
	 * @param taggedObject
	 * @return
	 */
	public static int delete(TaggedObject taggedObject) {
		int state = 0;
		try {
			int markId = theSession.setUndoMark(
					nxopen.Session.MarkVisibility.INVISIBLE, "Delete");
			theSession.updateManager().clearErrorList();
			theSession.updateManager().addToDeleteList(taggedObject);
			theSession.preferences().modeling().notifyOnDelete();
			state = theSession.updateManager().doUpdate(markId);
			theSession.deleteUndoMark(markId, null);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
			XmjlUtils.dialogError("删除失败！");
		}
		return state;
	}

	/**
	 * 隐藏指定对象
	 * 
	 * @param displayableObjects
	 */
	public static void hideObj(DisplayableObject... displayableObjects) {
		try {
			theSession.displayManager().blankObjects(displayableObjects);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
			XmjlUtils.dialogError("隐藏对象错误！");
		}
	}

	/**
	 * 打印指定点坐标
	 * 
	 * @param point
	 *            指定点
	 */
	public static void writePoint(Point3d point) {
		try {
			lw.open();
			lw.writeLine("x:" + point.x);
			lw.writeLine("y:" + point.y);
			lw.writeLine("z" + point.z);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 将点转换为double
	 * 
	 * @param point
	 * @return
	 */
	public static double[] convertPointToDouble(Point3d point) {
		return new double[] { point.x, point.y, point.z };
	}

	/**
	 * point3D转point
	 * 
	 * @param point3d
	 * @return
	 */
	public static Point point3dToPoint(Point3d point3d) {
		Point point = null;
		try {
			point = workPart.points().createPoint(point3d);
		} catch (RemoteException | NXException e) {
			e.printStackTrace();
		}
		return point;
	}

	/**
	 * 获取tag标签对应的对象
	 * 
	 * @param tag
	 * @return
	 * @throws RemoteException
	 */
	public static TaggedObject getTaggedObjectByTag(Tag tag)
			throws RemoteException {
		return theSession.taggedObjectManager().get(tag);
	}

	/**
	 * 获取指定图层的所有对象
	 * 
	 * @param layer
	 * @return
	 */
	public static List<TaggedObject> getAllTaggedObjectInlayer(int layer) {
		List<TaggedObject> list = new ArrayList<>();
		if (layer > 0 && layer <= 256) {
			Tag object = Tag.NULL;
			try {
				do {
					object = ufSession.layer().cycleByLayer(layer, object);
					if (object.equals(Tag.NULL)) {
						break;
					} else {
						list.add(XmjlUtils.getTaggedObjectByTag(object));
					}
				} while (!object.equals(Tag.NULL));
			} catch (RemoteException | NXException e) {
				e.printStackTrace();
			}
		} else {
			dialogError("图层不存在！");
		}
		return list;
	}

	/**
	 * 获取曲线所在的视图
	 * 
	 * @param curve
	 * @return
	 * @throws Exception
	 */
	public static DraftingView getView(DraftingCurve curve) throws Exception {
		Iterator viewIterator = workPart.draftingViews().iterator();
		while (viewIterator.hasNext()) {
			DraftingView draftingView = (DraftingView) viewIterator.next();
			Iterator bodyIterator = draftingView.draftingBodies().iterator();
			while (bodyIterator.hasNext()) {
				DraftingBody draftingBody = (DraftingBody) bodyIterator.next();
				Iterator draftingIterator = draftingBody.draftingCurves()
						.iterator();
				while (draftingIterator.hasNext()) {
					DraftingCurve draftingCurve = (DraftingCurve) draftingIterator
							.next();
					if (draftingCurve != null && draftingCurve.equals(curve)) {
						return draftingView;
					}
				}
			}
		}
		return null;
	}

	/**
	 * 将模型点位转换成视图点位
	 * 
	 * @param view
	 * @param point3d
	 * @return
	 * @throws Exception
	 */
	public static Point3d mapModelToDrawing(View view, Point3d point3d)
			throws Exception {
		double[] point = { point3d.x, point3d.y, point3d.z };
		double[] mapPoint = ufSession.view().mapModelToDrawing(view.tag(),
				point);
		return new Point3d(mapPoint[0], mapPoint[1], 0);
	}
	/**
	 * 将视图点位转换成模型点位
	 * @param view
	 * @param point3d
	 * @return
	 * @throws Exception
	 */
	public static Point3d mapDrawingToModel(View view, Point3d point3d)
			throws Exception {
		double[] point = { point3d.x, point3d.y};
		double[] mapPoint = ufSession.view().mapDrawingToModel(view.tag(), point);
		return new Point3d(mapPoint[0], mapPoint[1], mapPoint[2]);
	}
	/**
	 * 在指定视图上绘制直线
	 * @param point3d1
	 * @param point3d2
	 * @param view
	 * @throws Exception
	 */
	public static void createLine(Point3d point3d1,Point3d point3d2,View view) throws Exception{
		Point3d point1 = mapModelToDrawing(view,point3d1);
		Point3d point2 = mapModelToDrawing(view,point3d2);
		workPart.curves().createLine(point1, point2);
	}
}
