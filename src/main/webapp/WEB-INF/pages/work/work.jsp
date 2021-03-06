<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@include file="../common/head.jsp"%>
<style>
.modal {
	overflow: auto;
}
</style>
<script type="text/javascript" src="${pageContext.request.contextPath }/statics/localjs/DateFormat.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath }/statics/localjs/work.js"></script>

<!-- 内部main内容 -->
<div id="content" class="box col-lg-10 col-sm-10 ">
	<div class="row clearfix">
		<div class="col-md-12 column">
			<div class="tabbable" >
				<ul class="nav nav-pills ">
					<li class="active"><a href="#unallocated" data-toggle="tab" id="unallocated_tab">待分配</a></li>
					<li><a href="#follow" data-toggle="tab" id="follow_tab">跟进中</a></li>
					<li><a href="#completed" data-toggle="tab" id="completed_tab">已完成</a></li>
					<li class="pull-right">
					<shiro:hasPermission name="work_insert">
					<a class="btn btn-primary" data-toggle="modal" data-target="#insert_order_modal">
					
					添加业务单</a>
					</shiro:hasPermission>
					</li>
					<li class="pull-right"><a style="background-color: white;">
							<i class="font green glyphicon glyphicon-star">延后</i>
					</a></li>

					<li class="pull-right"><a style="background-color: white;">
							<i class="font yellow glyphicon glyphicon-star">正常</i>
					</a></li>

					<li class="pull-right"><a style="background-color: white;">
							<i class="font red glyphicon glyphicon-star">紧急</i>
					</a></li>
				</ul>
				<!-- 导航栏对应的内容 -->
				<div class="tab-content">
					<!-- 待分配选项卡  unallocated_order_tab-->
					<%@ include file="tab-pane/unallocated_order_tabpane.jsp"%>

					<!-- 跟进中选项卡  follow_order_tab-->
					<%@ include file="tab-pane/follow_order_tabpane.jsp"%>

					<!-- 已完成选项卡  completed_order_tab-->
					<%@ include file="tab-pane/completed_order_tabpane.jsp"%>
				</div>
			</div>
		</div>
	</div>
	<!-- 结束main内容 -->
</div>
<script type="text/javascript">
	var hasAdd=false;
	<shiro:hasPermission name="work_insert">hasAdd=true;
	</shiro:hasPermission>
	var hasDel=false;
	<shiro:hasPermission name="work_delete">hasDel=true;
	</shiro:hasPermission>
	var hasEdit=false;
	<shiro:hasPermission name="work_update">hasEdit=true;
	</shiro:hasPermission>
	var hasDis=false;
	<shiro:hasPermission name="work_distribution">hasDis=true;
	</shiro:hasPermission>
</script>
<!-- 业务单详情模态框（detailModal）-->
<%@ include file="work-modal/detail_order_modal.jsp"%>

<!-- 业务单编辑模态框（editModal） -->
<%@ include file="work-modal/edit_order_modal.jsp"%>

<!-- 业务单分配模态框（distributionModal） -->
<%@ include file="work-modal/distribution_order_modal.jsp"%>

<!-- 增加业务单模态框（addOrderModal） -->
<%@ include file="work-modal/insert_order_modal.jsp"%>

<%@include file="../common/foot.jsp"%>
<script type="text/javascript" src="${pageContext.request.contextPath }/statics/localjs/work_validate.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath }/statics/localjs/work_validate_method.js"></script>

