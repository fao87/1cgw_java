Последовательность создания сервисов Java, которые понимает 1C

Рассмотрим на примере создания сервиса TaskService

1 Создаём сервис в 1С, используя следующие соглашения:
При создании пакетов XDTO типы называем по английски. При создании имени типа используем следующее слглашение: тип дожен называться имя_типаType.

Пример TaskXDTO.xsd:

<xs:schema xmlns:tns="http://gw21c.cis.com" xmlns:xs="http://www.w3.org/2001/XMLSchema" targetNamespace="http://gw21c.cis.com" attributeFormDefault="unqualified" elementFormDefault="qualified">
	<xs:complexType name="TaskListType">
		<xs:sequence>
<xs:element name="tasks" type="tns:TaskType" nillable="true" minOccurs="0" maxOccurs="unbounded"/>
		</xs:sequence>
	</xs:complexType>
	<xs:complexType name="TaskType">
		<xs:sequence>
			<xs:element name="name" type="xs:string" nillable="true"/>
			<xs:element name="description" type="xs:string" nillable="true"/>
		</xs:sequence>
	</xs:complexType>
</xs:schema>

Код сервиса:

Функция TaskList()
	Запрос = Новый Запрос;
	Запрос.Текст = "ВЫБРАТЬ
		|	ЗадачаИсполнителя.Автор,
		|	ЗадачаИсполнителя.Дата,
		|	ЗадачаИсполнителя.Выполнена,
		|	ЗадачаИсполнителя.Предмет,
		|	ЗадачаИсполнителя.Наименование
		|ИЗ
		|	Задача.ЗадачаИсполнителя КАК ЗадачаИсполнителя";
	
	Выборка = Запрос.Выполнить().Выбрать();

	// Вставить содержимое обработчика.
	TaskListType = ФабрикаXDTO.Тип("http://gw21c.cis.com", "TaskListType");
	TaskList = ФабрикаXDTO.Создать(TaskListType);
	
	TaskType = ФабрикаXDTO.Тип("http://gw21c.cis.com", "TaskType");
	
	Пока Выборка.Следующий() Цикл
		Task = ФабрикаXDTO.Создать(TaskType);
		Task.name = Выборка.Наименование;
		TaskList.tasks.Добавить(Task);
	КонецЦикла;
	Возврат TaskList;
КонецФункции

Функция addTask(name, description) Экспорт
	Событие = Задачи.ЗадачаИсполнителя.СоздатьЗадачу();
	Событие.Автор = ПараметрыСеанса.ТекущийПользователь;
	Событие.Дата = ТекущаяДата();
	Событие.Выполнена = ложь;
	Событие.Предмет = description;
	Событие.Наименование = name;
	Событие.Записать();	
	Возврат "success";
КонецФункции

2. В 1С создаём клиентскую часть для работы с сервисом и проверяем корректность его работы.

Пример клиентской части:

WS = WSСсылки.WSTaskList.СоздатьWSПрокси("http://gw21c.cis.com","TaskService","TaskServiceSoap");
lst = WS.TaskList().tasks;
str = "";
Для i = 0 по lst.Количество() - 1 Цикл 
str = str + ", " + lst.Получить(i).name;	  		
КонецЦикла;

3. Для создания на стороне Java используем https://github.com/apache/axis2-java сервлет. (Необходмо дополнить библиотеки Axis2, входящие в стандартую поставку)

В папке App создаём следующую структуру:
\---src
    \---com
	  \---cis
		\---gw21c			    
			TaskType.java
			TaskListType.java
			TasjService.java

\---webapp
    \---WEB-INF
         |   web.xml
         |
         \---services
               \---TaskService
                     \---META-INF
    		            TaskService.wsdl
                             services.xml

В web.xml добавляем поддержку AxisServlet:
    <servlet>
        <servlet-name>AxisServlet</servlet-name>
        <display-name>Apache-Axis Servlet</display-name>
        <servlet-class>org.apache.axis2.transport.http.AxisServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>AxisServlet</servlet-name>
        <url-pattern>/services/*</url-pattern>
    </servlet-mapping>

Файл services.xml содержит конфигурацию сервиса:

<?xml version="1.0" encoding="UTF-8"?>
<serviceGroup>
    <service name="TaskService" scope="application">
        <description>TaskService service</description>
        <messageReceivers>
            <messageReceiver mep="http://www.w3.org/2004/08/wsdl/in-only" class="org.apache.axis2.rpc.receivers.RPCInOnlyMessageReceiver"/>
            <messageReceiver mep="http://www.w3.org/2004/08/wsdl/in-out" class="org.apache.axis2.rpc.receivers.RPCMessageReceiver"/>
        </messageReceivers>
        <parameter name="ServiceClass">com.cis.gw21c.TaskService</parameter>
    </service>
</serviceGroup>

TaskService.wsdl берём путём получения через браузер wsdl описания:

Папка src содержит исходный код объектов, созданный в соответствии с созданным в 1С описанием.

TaskType.java:
package com.cis.gw21c;

public class TaskType {

    private String name;

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }
    
}

TaskListType.java:
package com.cis.gw21c;

import java.util.List;

public class TaskListType {

    private List<TaskType> tasks;

    public List<TaskType> getTasks() {
        return this.tasks;
    }

    public void setTasks(List<TaskType> value) {
        this.tasks = value;
    }

}

TaskService.java:
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cis.gw21c;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;

import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericDispatcher;

import org.ofbiz.base.util.UtilDateTime;
/**
 */
public class TaskService {
    
    private GenericDelegator delegator;
    private LocalDispatcher dispatcher;
    
    public TaskService() {
    	delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
    	dispatcher = GenericDispatcher.getLocalDispatcher("default", delegator);    	
    }
    
    public TaskListType TaskList() {
    	
    	
    	try {
    		List<GenericValue> lst = delegator.findList("WorkEffortAndPartyAssign", null, null, null, null, false);
    	
    		TaskListType taskList = new TaskListType();

    		List<TaskType> lstTask = new ArrayList<TaskType>();
        
    		for(GenericValue item: lst) {
    			TaskType task = new TaskType();
        		task.setName(item.getString("workEffortName"));
        		lstTask.add(task);
    		}    		
    		taskList.setTasks(lstTask);
        
    		return taskList;
    	} catch (Exception ex) {
    		ex.printStackTrace();
    		return null;
    	}
    }
    
    private String getId(String id) {
    	return "GW21C-" + id;
    }
    
    // Если задача с id не найдена добваляет её в противном случае изменяет запись
    public String changeTask(String id, String name, String description) {    	
    	try {
    		String sysId = getId(id);
    		GenericValue we = delegator.makeValue("WorkEffort");    		
    		we.set("workEffortId", sysId);    		
    		we.set("workEffortTypeId", "TASK");
    		we.set("currentStatusId", "CAL_ACCEPTED");    		    		
    		we.set("workEffortName", name);
    		we.set("description", description);   
    		we.set("estimatedStartDate", UtilDateTime.toDate("01/01/2011 00:00:00"));
    		we.set("estimatedCompletionDate", UtilDateTime.toDate("01/01/2014 00:00:00"));
    		delegator.createOrStore(we);
    		
    		//WorkEffortPartyAssignment
    		we = delegator.makeValue("WorkEffortPartyAssignment");
    		we.set("workEffortId", sysId);
    		we.set("roleTypeId", "CAL_OWNER");    		
    		we.set("partyId", "admin");
    		we.set("statusId", "PRTYASGN_ASSIGNED");
    		we.set("statusDateTime", new Timestamp(System.currentTimeMillis()));
    		we.set("fromDate", UtilDateTime.toDate("01/01/2011 00:00:00"));
    		delegator.createOrStore(we);   
    		return sysId;
    	} catch (Exception ex) {
    		return "OFBiz Error: " + ex.getMessage();
    	}
    }
    
    // Удаление задачи
    public String removeTask(String id) {
    	String sysId = getId(id);    	
    	try {
    		
    		GenericValue we = delegator.makeValue("WorkEffort");
    		we.set("workEffortId", sysId);
    		we.removeRelated("WorkEffortKeyword");
    		we.removeRelated("WorkEffortPartyAssignment");
    		we.removeRelated("WorkEffortStatus");
    		we.remove();
    		return sysId;
    	} catch (Exception ex) {
    		return "OFBiz Error: " + ex.getMessage();
    	}
    }

}

Методы для работы с этим сервисом из 1С будут выглядеть примерно так:

Процедура addTaskНажатие(Элемент)
	WS = WSСсылки.TaskService.СоздатьWSПрокси("http://gw21c.cis.com","TaskService","TaskServiceHttpSoap12Endpoint");	
	res = WS.changeTask("3", "task-3", "description-1");	
КонецПроцедуры

Процедура RemoveTaskНажатие(Элемент)
	WS = WSСсылки.TaskService.СоздатьWSПрокси("http://gw21c.cis.com","TaskService","TaskServiceHttpSoap12Endpoint");	
	res = WS.removeTask("1");	
КонецПроцедуры

Создание клиентской OFBiz части к сервису 1C.

 Cервис 1С требует авторизацию до отправки WSDL файла. Обходится это использованием локального WSDL файла. Затем происходит инициализация сервиcа, выставление его прав авторизации.

Пример:
    private static TaskListType taskList() throws MalformedURLException, IOException {
        URL url = new URL("file:" + ctx.getRealPath("/WEB-INF/") + "/" + "TaskService.wsdl");
        com.cis.remotetask.TaskService service =  new com.cis.remotetask.TaskService(
                url, new QName("http://gw21c.cis.com", "TaskService")
                );
        com.cis.remotetask.TaskServicePortType port = service.getTaskServiceSoap();

        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "admin");
        bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "111");

        return port.taskList(); 
    }

Пример кода клиента вебсервиса на офбизе рассмотрено на примере вызова метода создания задачи в 1С (метод addTask):

Пакет com.cis.gw21c.client

TaskClientService.java

package com.cis.gw21c.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.namespace.QName;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;


public class TaskClientService {

	public static Map<String, Object> gw21cAddTask(DispatchContext dctx, Map context) {
		GenericDelegator delegator = (GenericDelegator) dctx.getDelegator();
		String name = (String) context.get("workEffortName");
		String description = (String) context.get("description");
		if (name == null) {
			name = ""; 
		}
		if (description == null) {
			description = "";
		}
		try {
			Map result = ServiceUtil.returnSuccess();
			URL url = new URL("file:TaskService.xml");
	        com.cis.gw21c.client.TaskService service = new com.cis.gw21c.client.TaskService(
	                url, new QName("http://gw21c.cis.com", "TaskService"));
	        com.cis.gw21c.client.TaskServicePortType port = service.getTaskServiceSoap();
	        BindingProvider bp = (BindingProvider) port;
	        bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "admin");
	        bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "111");
	        result.put("result", port.addTask(name, description));
			return result;
		} catch (Exception ex) {
			return ServiceUtil.returnError("Error: " + ex.getMessage());
		}
		
	}

}

Полный код примера находится в каталоге ofbiz/hot-deploy/gw21c/src/com/cis/gw21c/client

Интеграция данного сервиса в событие офбиз:

1. В файл описания сервисов модуля gw21c/servicedef/services.xml добавляем описание сервиса gw21cAddTask, предназначенного для связи вышенаписанного кода с системой событий на Java.

    <service name="gw21cAddTask" engine="java" export="true" invoke="gw21cAddTask"
    	location="com.cis.gw21c.client.TaskClientService">
    	<attribute name="workEffortName" mode="IN" type="String" optional="false" />
    	<attribute name="description" mode="IN" type="String" optional="true" />
    	<attribute name="currentStatusId" mode="IN" type="String" optional="true" />
    	<attribute name="actualCompletionDate" mode="IN" type="java.sql.Timestamp" optional="true" />
    	<attribute name="actualStartDate" mode="IN" type="java.sql.Timestamp" optional="true" />
    	<attribute name="estimatedCompletionDate" mode="IN" type="java.sql.Timestamp" optional="true" />
    	<attribute name="estimatedStartDate" mode="IN" type="java.sql.Timestamp" optional="true" />
    	<attribute name="fixedAssetId" mode="IN" type="String" optional="true" />
    	<attribute name="priority" mode="IN" type="String" optional="true" />
    	<attribute name="scopeEnumId" mode="IN" type="String" optional="true" />
    	<attribute name="workEffortTypeId" mode="IN" type="String" optional="true" />
    	
    	<attribute name="result" mode="OUT" type="String" optional="false" />
    </service>

С этого момента  клиентский код можно запускать из интерфейса (webtools->service->runService).

2. Добавляем данный код в определение сервиса события в нашем случае это будет файл ofbiz/applications/workeffort/script/org/ofbiz/workeffort/workeffort/ WorkEffortSimpleServices.xml. Вставляемые строки в фрагменте отмечены жирным.

<simple-method method-name="createWorkEffortAndPartyAssign" short-description="Create Work Effort and assign to a party with a role">
        <set-service-fields service-name="createWorkEffort" map="parameters" to-map="create"/>
        <call-service service-name="createWorkEffort" in-map-name="create">
            <result-to-field result-name="workEffortId"/>
        </call-service>
        
        <call-service service-name="gw21cAddTask" in-map-name="create">
            <result-to-field result-name="result"/>
        </call-service>
        
        <set-service-fields service-name="assignPartyToWorkEffort" map="parameters" to-map="assign"/>
        <set field="assign.workEffortId" from-field="workEffortId"/>
        <call-service service-name="assignPartyToWorkEffort" in-map-name="assign"/>
        <field-to-result field="workEffortId"/>
    </simple-method>

С этого момента TaskClientService начинает работать как событие.
