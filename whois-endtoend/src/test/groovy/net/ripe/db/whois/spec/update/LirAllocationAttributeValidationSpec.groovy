package net.ripe.db.whois.spec.update

import net.ripe.db.whois.common.IntegrationTest
import net.ripe.db.whois.spec.BaseQueryUpdateSpec

@org.junit.experimental.categories.Category(IntegrationTest.class)
class LirAllocationAttributeValidationSpec extends BaseQueryUpdateSpec {

    @Override
    Map<String, String> getTransients() {
        ["ALLOC-PA-MANDATORY"            : """\
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                source:       TEST
                """,
         "ALLOC-PA-EXTRA"                : """\
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                descr:        some description  # extra
                country:      NL
                geoloc:       0.0 0.0           # extra
                language:     NL                # extra
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                remarks:      a new remark      # extra
                notify:       notify@ripe.net   # extra
                mnt-lower:    LIR-MNT           # extra
                mnt-routes:   OWNER-MNT         # extra
                mnt-domains:  DOMAINS-MNT       # extra
                mnt-irt:      IRT-TEST          # extra
                source:       TEST
                """,
         "ALLOC-PA-RIPE-NCC-MNTNER": """\
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                mnt-lower:    RIPE-NCC-HM-MNT  # hm-mnt
                mnt-routes:   RIPE-NCC-HM-MNT  # hm-mnt
                mnt-domains:  RIPE-NCC-HM-MNT  # hm-mnt
                source:       TEST
                """,
         "ALLOC-PA-EXTRA-RIPE-NCC-MNTNER": """\
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                mnt-lower:    RIPE-NCC-HM-MNT  # hm-mnt
                mnt-lower:    LIR-MNT          # extra
                mnt-routes:   RIPE-NCC-HM-MNT  # hm-mnt
                mnt-routes:   OWNER-MNT        # extra
                mnt-domains:  RIPE-NCC-HM-MNT  # hm-mnt
                mnt-domains:  DOMAINS-MNT      # extra
                source:       TEST
                """,
         "IRT"                           : """\
                irt:          irt-test
                address:      RIPE NCC
                e-mail:       dbtest@ripe.net
                signature:    PGPKEY-D83C3FBD
                encryption:   PGPKEY-D83C3FBD
                auth:         PGPKEY-D83C3FBD
                auth:         MD5-PW \$1\$qxm985sj\$3OOxndKKw/fgUeQO7baeF/  #irt
                irt-nfy:      dbtest@ripe.net
                notify:       dbtest@ripe.net
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                mnt-by:       OWNER-MNT
                source:       TEST
                """,
         "IRT2"                          : """\
                irt:          IRT-2-TEST
                address:      RIPE NCC
                e-mail:       dbtest@ripe.net
                signature:    PGPKEY-D83C3FBD
                encryption:   PGPKEY-D83C3FBD
                auth:         PGPKEY-D83C3FBD
                auth:         MD5-PW \$1\$qxm985sj\$3OOxndKKw/fgUeQO7baeF/  #irt
                irt-nfy:      dbtest@ripe.net
                notify:       dbtest@ripe.net
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                mnt-by:       OWNER-MNT
                source:       TEST
                """,
         "DOMAINS2-MNT"                  : """\
                mntner:      DOMAINS2-MNT
                descr:       used for mnt-domains
                admin-c:     TP1-TEST
                upd-to:      updto_domains@ripe.net
                mnt-nfy:     mntnfy_domains@ripe.net
                notify:      notify_domains@ripe.net
                auth:        MD5-PW \$1\$anTWxMgQ\$8aBWq5u5ZFHLA5aeZsSxG0  #domains
                mnt-by:      DOMAINS2-MNT
                source:      TEST
                """
        ]
    }

    //  MODIFY allocations attributes by LIR

    def "modify inetnum, add (all) lir-unlocked attributes by lir"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-MANDATORY") + "override: denis, override1")
        syncUpdate(getTransient("IRT") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")
        queryObject("-r -T irt irt-test", "irt", "irt-test")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                descr:        some description  # added
                country:      NL
                country:      DE                # added
                geoloc:       0.0 0.0           # added
                language:     NL                # added
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                admin-c:      TP2-TEST          # added
                tech-c:       TP1-TEST
                tech-c:       TP2-TEST          # added
                remarks:      a new remark      # added
                notify:       notify@ripe.net   # added
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                mnt-lower:    LIR2-MNT          # added
                mnt-routes:   OWNER-MNT         # added
                mnt-domains:  DOMAINS-MNT       # added
                mnt-irt:      IRT-TEST          # added
                source:       TEST
                password: lir
                password: irt
                """.stripIndent()
        )

        then:
        ack.success

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(1, 0, 1, 0, 0)
        ack.summary.assertErrors(0, 0, 0, 0)

        ack.countErrorWarnInfo(0, 0, 0)
        ack.successes.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
    }

    def "modify inetnum, change (all) lir-unlocked attributes by lir"() {
        given:
        syncUpdate(getTransient("IRT") + "override: denis, override1")
        syncUpdate(getTransient("IRT2") + "override: denis, override1")
        syncUpdate(getTransient("DOMAINS2-MNT") + "override: denis, override1")
        syncUpdate(getTransient("ALLOC-PA-EXTRA") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")
        queryObject("-r -T mntner DOMAINS2-MNT", "mntner", "DOMAINS2-MNT")
        queryObject("-r -T irt irt-test", "irt", "irt-test")
        queryObject("-r -T irt IRT-2-TEST", "irt", "IRT-2-TEST")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                descr:        other description # changed
                country:      DE                # changed
                geoloc:       9.0 9.0           # changed
                language:     DE                # changed
                org:          ORG-LIR1-TEST
                admin-c:      TP2-TEST          # changed
                tech-c:       TP2-TEST          # changed
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                remarks:      a different remark# changed
                notify:       other@ripe.net    # changed
                mnt-lower:    LIR2-MNT          # changed
                mnt-routes:   OWNER2-MNT        # changed
                mnt-domains:  DOMAINS-MNT       # changed
                mnt-irt:      IRT-2-TEST        # changed
                source:       TEST
                password: lir
                password: irt
                """.stripIndent()
        )

        then:
        ack.success

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(1, 0, 1, 0, 0)
        ack.summary.assertErrors(0, 0, 0, 0)
        ack.countErrorWarnInfo(0, 0, 0)
        ack.successes.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
    }

    def "modify inetnum, cannot change lir-locked attributes by lir"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-MANDATORY") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME-CHANGED # changed
                country:      NL
                org:          ORG-OTO1-TEST         # changed
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PI          # changed
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR2-MNT              # changed
                source:       TEST
                password: lir
                password: owner3
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)

        ack.countErrorWarnInfo(5, 0, 0)
        ack.errors.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }

        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "Referenced organisation can only be changed by the RIPE NCC for this resource. Please contact \"ncc@ripe.net\" to change this reference.",
                "Attribute \"mnt-by:\" can only be changed by the RIPE NCC for this object. Please contact \"ncc@ripe.net\" to change it.",
                "The \"netname\" attribute can only be changed by the RIPE NCC",
                "Referenced organisation has wrong \"org-type\". Allowed values are [IANA, RIR, LIR]",
                "status value cannot be changed, you must delete and re-create the object"
        ]
    }

    def "modify inetnum, cannot add sponsoring-org by lir"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-MANDATORY") + "override: denis, override1")

        expect:

        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                sponsoring-org: ORG-OTO1-TEST # added
                source:       TEST
                password: lir
                password: owner3
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)

        ack.countErrorWarnInfo(1, 0, 0)
        ack.errors.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }

        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "The \"sponsoring-org:\" attribute is not allowed with status value \"ALLOCATED PA\""
        ]
    }

    def "modify inetnum, cannot change (mnt-lower) ripe-ncc maintainer by lir"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-RIPE-NCC-MNTNER") + "override: denis, override1")
        syncUpdate(getTransient("IRT") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")
        queryObject("-r -T irt irt-test", "irt", "irt-test")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                mnt-lower:    LIR2-MNT          # changed
                mnt-routes:   RIPE-NCC-HM-MNT
                mnt-domains:  RIPE-NCC-HM-MNT
                source:       TEST
                password: lir
                password: irt
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)

        ack.countErrorWarnInfo(1, 0, 0)
        ack.errors.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "Adding or removing a RIPE NCC maintainer requires administrative authorisation"
        ]
    }

    def "modify inetnum, cannot add (mnt-lower) ripe-ncc maintainer by lir"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-RIPE-NCC-MNTNER") + "override: denis, override1")
        syncUpdate(getTransient("IRT") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")
        queryObject("-r -T irt irt-test", "irt", "irt-test")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                mnt-lower:    RIPE-NCC-HM-MNT
                mnt-lower:    RIPE-NCC-LEGACY-MNT          # added
                mnt-routes:   RIPE-NCC-HM-MNT
                mnt-domains:  RIPE-NCC-HM-MNT
                source:       TEST
                password: lir
                password: irt
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)

        ack.countErrorWarnInfo(1, 0, 0)
        ack.errors.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "Adding or removing a RIPE NCC maintainer requires administrative authorisation"
        ]
    }

    def "modify inetnum, cannot delete (mnt-lower) ripe-ncc maintainer by lir"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-EXTRA-RIPE-NCC-MNTNER") + "override: denis, override1")
        syncUpdate(getTransient("IRT") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")
        queryObject("-r -T irt irt-test", "irt", "irt-test")

        when:
        //      mnt-lower:   RIPE-NCC-HM-MNT  # cannot deleted
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                mnt-lower:    LIR-MNT          # extra
                mnt-routes:   RIPE-NCC-HM-MNT  # hm-mnt
                mnt-routes:   OWNER-MNT        # extra
                mnt-domains:  RIPE-NCC-HM-MNT  # hm-mnt
                mnt-domains:  DOMAINS-MNT      # extra
                source:       TEST
                password: lir
                password: irt
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)

        ack.countErrorWarnInfo(1, 0, 0)
        ack.errors.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "Adding or removing a RIPE NCC maintainer requires administrative authorisation"
        ]
    }

    def "modify inetnum, cannot change (mnt-routes) ripe-ncc maintainer by lir"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-RIPE-NCC-MNTNER") + "override: denis, override1")
        syncUpdate(getTransient("IRT") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")
        queryObject("-r -T irt irt-test", "irt", "irt-test")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                mnt-lower:    RIPE-NCC-HM-MNT
                mnt-routes:   LIR2-MNT          # changed
                mnt-domains:  RIPE-NCC-HM-MNT
                source:       TEST
                password: lir
                password: irt
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)

        ack.countErrorWarnInfo(1, 0, 0)
        ack.errors.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "Adding or removing a RIPE NCC maintainer requires administrative authorisation"
        ]
    }

    def "modify inetnum, cannot add (mnt-routes) ripe-ncc maintainer by lir"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-RIPE-NCC-MNTNER") + "override: denis, override1")
        syncUpdate(getTransient("IRT") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")
        queryObject("-r -T irt irt-test", "irt", "irt-test")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                mnt-lower:    RIPE-NCC-HM-MNT
                mnt-routes:   RIPE-NCC-HM-MNT
                mnt-routes:   RIPE-NCC-LEGACY-MNT          # added
                mnt-domains:  RIPE-NCC-HM-MNT
                source:       TEST
                password: lir
                password: irt
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)

        ack.countErrorWarnInfo(1, 0, 0)
        ack.errors.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "Adding or removing a RIPE NCC maintainer requires administrative authorisation"
        ]
    }

    def "modify inetnum, cannot delete (mnt-routes) ripe-ncc maintainer by lir"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-EXTRA-RIPE-NCC-MNTNER") + "override: denis, override1")
        syncUpdate(getTransient("IRT") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")
        queryObject("-r -T irt irt-test", "irt", "irt-test")

        when:
        //      mnt-routes:   RIPE-NCC-HM-MNT  # cannot deleted
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                mnt-lower:    RIPE-NCC-HM-MNT  # hm-mnt
                mnt-lower:    LIR-MNT          # extra
                mnt-routes:   OWNER-MNT        # extra
                mnt-domains:  RIPE-NCC-HM-MNT  # hm-mnt
                mnt-domains:  DOMAINS-MNT      # extra
                source:       TEST
                password: lir
                password: irt
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)

        ack.countErrorWarnInfo(1, 0, 0)
        ack.errors.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "Adding or removing a RIPE NCC maintainer requires administrative authorisation"
        ]
    }

    def "modify inetnum, cannot change (mnt-domains) ripe-ncc maintainer by lir"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-RIPE-NCC-MNTNER") + "override: denis, override1")
        syncUpdate(getTransient("IRT") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")
        queryObject("-r -T irt irt-test", "irt", "irt-test")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                mnt-lower:    RIPE-NCC-HM-MNT
                mnt-routes:   RIPE-NCC-HM-MNT
                mnt-domains:  LIR2-MNT          # changed
                source:       TEST
                password: lir
                password: irt
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)

        ack.countErrorWarnInfo(1, 0, 0)
        ack.errors.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "Adding or removing a RIPE NCC maintainer requires administrative authorisation"
        ]
    }

    def "modify inetnum, cannot add (mnt-domains) ripe-ncc maintainer by lir"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-RIPE-NCC-MNTNER") + "override: denis, override1")
        syncUpdate(getTransient("IRT") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")
        queryObject("-r -T irt irt-test", "irt", "irt-test")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                mnt-lower:    RIPE-NCC-HM-MNT
                mnt-routes:   RIPE-NCC-HM-MNT
                mnt-domains:  RIPE-NCC-HM-MNT
                mnt-domains:  RIPE-NCC-LEGACY-MNT          # added
                source:       TEST
                password: lir
                password: irt
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)

        ack.countErrorWarnInfo(1, 0, 0)
        ack.errors.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "Adding or removing a RIPE NCC maintainer requires administrative authorisation"
        ]
    }

    def "modify inetnum, cannot delete (mnt-domains) ripe-ncc maintainer by lir"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-EXTRA-RIPE-NCC-MNTNER") + "override: denis, override1")
        syncUpdate(getTransient("IRT") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")
        queryObject("-r -T irt irt-test", "irt", "irt-test")

        when:
        //      mnt-domains:   RIPE-NCC-HM-MNT  # cannot deleted
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                mnt-lower:    RIPE-NCC-HM-MNT  # hm-mnt
                mnt-lower:    LIR-MNT          # extra
                mnt-routes:   RIPE-NCC-HM-MNT  # hm-mnt
                mnt-routes:   OWNER-MNT        # extra
                mnt-domains:  DOMAINS-MNT      # extra
                source:       TEST
                password: lir
                password: irt
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)

        ack.countErrorWarnInfo(1, 0, 0)
        ack.errors.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "Adding or removing a RIPE NCC maintainer requires administrative authorisation"
        ]
    }

    def "modify inetnum, delete (all) lir-unlocked attributes by lir"() {
        given:
        syncUpdate(getTransient("IRT") + "override: denis, override1")
        syncUpdate(getTransient("ALLOC-PA-EXTRA") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")
        queryObject("-r -T irt irt-test", "irt", "irt-test")

        when:
        //        descr:        other description # deleted
        //        geoloc:       9.0 9.0           # deleted
        //        language:     DE                # deleted
        //        admin-c:      TP2-TEST          # deleted
        //        tech-c:       TP2-TEST          # deleted
        //        remarks:      a different remark# deleted
        //        notify:       other@ripe.net    # deleted
        //        mnt-lower:    LIR2-MNT          # deleted
        //        mnt-routes:   OWNER2-MNT        # deleted
        //        mnt-domains:  DOMAINS-MNT       # deleted
        //        mnt-irt:      IRT-2-TEST        # deleted
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                source:       TEST
                password: lir
                """.stripIndent()
        )

        then:
        ack.success

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(1, 0, 1, 0, 0)
        ack.summary.assertErrors(0, 0, 0, 0)
        ack.countErrorWarnInfo(0, 0, 0)
        ack.successes.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
    }

    def "modify inetnum, cannot delete (some) mandatory lir-unlocked attributes by lir"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-MANDATORY") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")

        when:
        //        org:          ORG-LIR1-TEST # cannot delete, but warning is NOT presented!!
        //        country:      NL            # cannot delete
        //        admin-c:      TP1-TEST      # cannot delete
        //        tech-c:       TP1-TEST      # cannot delete
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                source:       TEST
                password: lir
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)
        ack.countErrorWarnInfo(3, 0, 0)

        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "Mandatory attribute \"country\" is missing",
                "Mandatory attribute \"admin-c\" is missing",
                "Mandatory attribute \"tech-c\" is missing"]
    }

    def "modify inetnum, cannot delete (org) lir-unlocked attributes by lir"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-MANDATORY") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")

        when:
        //        org:          ORG-LIR1-TEST # cannot delete
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                source:       TEST
                password: lir
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)

        ack.countErrorWarnInfo(2, 0, 0)
        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "Referenced organisation can only be removed by the RIPE NCC for this resource. Please contact \"ncc@ripe.net\" to remove this reference.",
                "Missing required \"org:\" attribute"]
    }

    //  MODIFY allocations attributes WITH OVERRIDE

    def "modify inetnum, change lir-locked attributes with override"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-MANDATORY") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME-CHANGED # changed
                country:      NL
                org:          ORG-LIRA-TEST         # changed
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PI          # changed
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR2-MNT              # changed
                source:       TEST
                override:     denis,override1
                """.stripIndent()
        )

        then:
        ack.success

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(1, 0, 1, 0, 0)
        ack.summary.assertErrors(0, 0, 0, 0)
        ack.countErrorWarnInfo(0, 0, 1)
        ack.successes.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
    }

    //  MODIFY allocations attributes WITH RS PASSWORD

    def "modify inetnum, change lir-locked attributes with rs password"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-MANDATORY") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME-CHANGED # changed
                country:      NL
                org:          ORG-LIRA-TEST         # changed
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR2-MNT              # changed
                source:       TEST
                password: hm
                password: lir
                password: owner3
                """.stripIndent()
        )

        then:
        ack.success

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(1, 0, 1, 0, 0)
        ack.summary.assertErrors(0, 0, 0, 0)
        ack.countErrorWarnInfo(0, 0, 0)
        ack.successes.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
    }

    def "modify inetnum, change lir-locked (status) attributes with rs password"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-MANDATORY") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PI         # changed
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                source:       TEST
                password: hm
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)
        ack.countErrorWarnInfo(1, 0, 0)
        ack.errors.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "status value cannot be changed, you must delete and re-create the object"
        ]
    }

    def "modify inetnum, add 'single' attributes with rs password"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-MANDATORY") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                netname:      TEST-NET-NAME-2      # added
                country:      NL
                org:          ORG-LIR1-TEST
                org:          ORG-OTO1-TEST        # added
                sponsoring-org: ORG-OTO1-TEST      # added
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                status:       ALLOCATED PI         # added
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                source:       TEST
                password: hm
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)
        ack.countErrorWarnInfo(3, 0, 0)
        ack.errors.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "Attribute \"netname\" appears more than once",
                "Attribute \"org\" appears more than once",
                "Attribute \"status\" appears more than once"
        ]
    }

    def "modify inetnum, add sponsoring attributes with rs password"() {
        given:
        syncUpdate(getTransient("ALLOC-PA-MANDATORY") + "override: denis, override1")

        expect:
        queryObject("-GBr -T inetnum 192.168.0.0 - 192.169.255.255", "inetnum", "192.168.0.0 - 192.169.255.255")

        when:
        def ack = syncUpdateWithResponse("""
                inetnum:      192.168.0.0 - 192.169.255.255
                netname:      TEST-NET-NAME
                country:      NL
                org:          ORG-LIR1-TEST
                sponsoring-org: ORG-OTO1-TEST      # added
                admin-c:      TP1-TEST
                tech-c:       TP1-TEST
                status:       ALLOCATED PA
                mnt-by:       RIPE-NCC-HM-MNT
                mnt-by:       LIR-MNT
                source:       TEST
                password: hm
                """.stripIndent()
        )

        then:
        ack.errors

        ack.summary.nrFound == 1
        ack.summary.assertSuccess(0, 0, 0, 0, 0)
        ack.summary.assertErrors(1, 0, 1, 0)
        ack.countErrorWarnInfo(1, 0, 0)
        ack.errors.any { it.operation == "Modify" && it.key == "[inetnum] 192.168.0.0 - 192.169.255.255" }
        ack.errorMessagesFor("Modify", "[inetnum] 192.168.0.0 - 192.169.255.255") == [
                "The \"sponsoring-org:\" attribute is not allowed with status value \"ALLOCATED PA\""]
    }
}