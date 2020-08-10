<%--
  CDDL HEADER START
  The contents of this file are subject to the terms
  of the Common Development and Distribution License
  (the License). You may not use this file except in
  compliance with the License.

  You can obtain a copy of the License at
  http://www.sun.com/cddl/cddl.html and legal/CDDLv1.0.txt
  See the License for the specific language governing
  permission and limitations under the License.

  When distributing Covered Code, include this CDDL
  Header Notice in each file and include the License file
  at legal/CDDLv1.0.txt.
  If applicable, add the following below the CDDL Header,
  with the fields enclosed by brackets [] replaced by
  your own identifying information:
  "Portions Copyrighted [year] [name of copyright owner]"

  Copyright 2006 Sun Microsystems Inc. All Rights Reserved
  CDDL HEADER END
--%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<jsp:include page="header.jsp" flush="true"/>
  
<jsp:include page="tabs.jsp" flush="true">
  <jsp:param name="selectedTab" value="admin" />
</jsp:include>
  
<div id="portal-content">
<%-- DEPLOYING/UNDEPLOYING PORTLETS REQUIRE LOGIN (ADDED BY AHMED, 2007-10-13) --%>
<%
	if (request.getRemoteUser() != null)
		request.setAttribute("portletRemoteUser", request.getRemoteUser());
%>

<c:choose>

	<c:when test="${requestScope.portletRemoteUser != null}">
	  <jsp:include page="deploy-portlet.jsp" flush="true" />
	  <jsp:include page="undeploy-portlet.jsp" flush="true" />
	  <jsp:include page="create-portlet.jsp" flush="true" />
	</c:when>
	<c:otherwise>
		You must be logged in to deploy/undeploy portlets.
	</c:otherwise>

</c:choose>

</div> <!-- closes portal-content -->

</div> <!-- closes portal-page -->
</body>
</html>