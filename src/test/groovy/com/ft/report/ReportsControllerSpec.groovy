package com.ft.report

import com.ft.report.model.Criteria
import com.ft.report.model.Desk
import com.ft.report.model.Project
import com.ft.report.model.Report
import com.ft.report.model.ReportType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.ui.ModelMap
import spock.lang.Specification
import spock.lang.Unroll
import sun.security.krb5.internal.crypto.Des

import java.time.Clock
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId

class ReportsControllerSpec extends Specification {
    private static final LocalDateTime FRIDAY_EVENING = LocalDateTime.of(2015, Month.JUNE, 5, 15, 0)
    private static final LocalDateTime MONDAY_EVENING = LocalDateTime.of(2015, Month.JUNE, 1, 15, 0)
    private static final LocalDateTime MONDAY_MORNING = LocalDateTime.of(2015, Month.JUNE, 1, 10, 0)
    private static final ZoneId zoneId = ZoneId.systemDefault()

    private ReportsController controller

    void setup() {
        controller = new ReportsController()

        Project project1 = new Project(id: 1, name: 'project 1')
        Project project2 = new Project(id: 2, name: 'project 2')
        Desk desk1 = new Desk(projects: [project1])
        Desk desk2 = new Desk(projects: [project2])
        controller.desks = ['one': desk1, 'two': desk2]
    }

    void 'populate report types'() {
        when:
            ReportType[] result = controller.populateReportTypes()
        then:
            result.size() == 3
            result == ReportType.values()
    }

    @Unroll
    void "populatePreferredReportType - date: #date"() {
        given:
            controller.clock = Clock.fixed(date.atZone(zoneId).toInstant(), zoneId)

        when:
            ReportType result = controller.populatePreferredReportType()

        then:
            result == expectedResult

        where:
            date           | expectedResult
            MONDAY_MORNING | ReportType.TODAY
            MONDAY_EVENING | ReportType.TOMORROW
            FRIDAY_EVENING | ReportType.SUNDAY_FOR_MONDAY
    }


    @Unroll
    void "buildReportDate for report type: #reportType"() {
        given:
            controller.clock = Clock.fixed(MONDAY_MORNING.atZone(zoneId).toInstant(), zoneId)

        when:
            String result = controller.buildReportDate(preferredReportType)

        then:
            result == expectedReportDate

        where:
            preferredReportType          | expectedReportDate
            ReportType.TODAY             | '01/06/2015'
            ReportType.TOMORROW          | '02/06/2015'
            ReportType.SUNDAY_FOR_MONDAY | '07/06/2015'
    }

    @Unroll
    void "home - #scenario"() {
        given:
            ReportType preferredReportType = ReportType.SUNDAY_FOR_MONDAY
            Map<String, Object> model = [:]
            Criteria expectedCriteria = new Criteria(reportType: preferredReportType, team: expectedTeam, project: expectedProject)

        when:
            String viewName = controller.home(teams, preferredReportType, model)

        then:
            viewName == 'reports/home'
            model['criteria'] == expectedCriteria

        where:
            scenario              | teams                  | expectedTeam | expectedProject
            'user with no teams'  | [:]                    | null         | null
            'user with two teams' | ['one': [], 'two': []] | 'one'        | new Project(id: 1, name: 'project 1')
    }

    void "create Sunday for Monday report"() {
        given:
            String team = 'one'
            ReportGenerator mockDefaultReportGenerator = Mock(ReportGenerator)
            controller.reportGenerator = mockDefaultReportGenerator
        and:
            Criteria criteria = new Criteria(team: team, reportType: ReportType.SUNDAY_FOR_MONDAY, project: new Project(id: 1, name: 'project 1'))
            ModelMap modelMap = new ModelMap()
            Report expectedReport = new Report()

        when:
            String viewName = controller.create(criteria, modelMap)

        then:
            1 * mockDefaultReportGenerator.generate(criteria) >> expectedReport
            0 * _
        and:
            viewName == 'reports/home'
            modelMap['criteria'] == criteria
            modelMap['report'] == expectedReport
    }
    void "user teams are populated"(){
        given:
            def mockSecurityContextHolder = Spy(SecurityContextHolder)
            def mockSecurityContext = Mock(SecurityContext)
            mockSecurityContextHolder.context = mockSecurityContext
        and:
            def mockOAuth2Authentication = Mock(OAuth2Authentication)
            def mockAuthentication = Mock(Authentication)
            def userDetails = [:]

        when:
            def teams = controller.populateUserTeams()

        then:
            1 * mockSecurityContext.getAuthentication() >> mockOAuth2Authentication
            1 * mockOAuth2Authentication.getUserAuthentication() >> mockAuthentication
            1 * mockAuthentication.getDetails() >> userDetails

        and:
            notThrown(NullPointerException)
            teams == Collections.emptyMap()
    }
}
