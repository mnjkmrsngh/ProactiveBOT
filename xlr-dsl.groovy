/**
 * The max commit message length
 */
import hudson.model.*
import hudson.util.*
import jenkins.model.*
import hudson.FilePath
import hudson.node_monitors.*
import java.time.LocalDateTime
final static int MAX_COMMIT_MSG_LEN = 100

 /**
 * Build the list of Changes triggering this Jenkins build, including any upstreams Jenkins jobs <br/>
 * @param s Current build object, it's a 'this' call from the pipeline
 * @return A string with all the changes in the current and upstream build, 'no new changes' is returned if empty
 */
static buildChangeList(s) {
    def changes = ''

    Run<?, ?> cur = s.currentBuild.rawBuild
    Cause.UpstreamCause upc = cur.getCause(Cause.UpstreamCause.class)
    while (upc != null) {
        Job<?, ?> p = (Job<?, ?>) Jenkins.getActiveInstance().getItemByFullName(upc.getUpstreamProject())
        if (p == null) {
            s.echo 'There is a break in the build linkage, could not retrieve upstream build information'
            break
        }
        cur = p.getBuildByNumber(upc.getUpstreamBuild());
        if (cur == null) {
            s.echo 'There is a break in the build linkage, could not retrieve upstream build information'
            break
        }
        changes += "\nJenkins Trigger Job - $upc.upstreamProject"
        changes +=  retrieveChangeSet(cur.changeSets)

        upc = cur.getCause(Cause.UpstreamCause.class)
    }

    if (!changes) { // no upstream changes at all, see if current build has any changes
        def currentBuildChanges = retrieveChangeSet(s.currentBuild.changeSets)

        changes = currentBuildChanges ?: '\n - No new changes'  // if no current build changes, use default message
    }
    return changes
}

/**
 * Retrieve all the change sets available. Include the git author, commit message and commit id
 * @param changeSets The changeSet object from Jenkins
 * @return A string with all the changes found within the change set
 */
static retrieveChangeSet(changeSets) {
    def changes = ''

    for (int i = 0; i < changeSets.size(); i++) {   // iterate through all the available change sets
        for (int j = 0; j < changeSets[i].items.length; j++) { // iterate through all the items of a single changeset
            def entry = changeSets[i].items[j]
            def commitmsg = entry.msg.take(MAX_COMMIT_MSG_LEN)
            changes += "\n[${entry.author}] - ${commitmsg}  -  Commit ${entry.commitId}"
        }
    }
    return changes
}
