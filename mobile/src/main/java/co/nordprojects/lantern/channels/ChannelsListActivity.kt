package co.nordprojects.lantern.channels

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.Snackbar.LENGTH_LONG
import co.nordprojects.lantern.App
import co.nordprojects.lantern.R
import co.nordprojects.lantern.channels.config.ChannelConfigActivity
import co.nordprojects.lantern.channels.config.ChannelConfigActivities
import co.nordprojects.lantern.home.HomeActivity
import co.nordprojects.lantern.shared.ChannelConfiguration
import co.nordprojects.lantern.shared.ChannelInfo
import co.nordprojects.lantern.shared.Direction
import kotlinx.android.synthetic.main.activity_channels_list.*
import org.json.JSONObject
import java.util.*

class ChannelsListActivity : AppCompatActivity(),
        ChannelListFragment.OnChannelSelectedListener {

    private var direction: Direction = Direction.FORWARD
    private var projectorObserver = Observer({_, _ -> projectorDidUpdate()})

    companion object {
        val TAG: String = ChannelsListActivity::class.java.simpleName
        val CONFIG_ACTIVITY_REQUEST = 1
        const val ARG_DIRECTION = "direction"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channels_list)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(R.drawable.back_chevron)

        val directionString = intent.getStringExtra(ARG_DIRECTION)
        direction = Direction.valueOf(directionString)


        val channelFragment = ChannelListFragment()
        val args = Bundle()
        args.putString(ARG_DIRECTION, directionString)
        channelFragment.arguments = args
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragmentContainer, channelFragment)
            commit()
        }
    }

    override fun onPause() {
        super.onPause()
        App.instance.projector?.deleteObserver(projectorObserver)
    }

    override fun onChannelSelected(channel: ChannelInfo) {
        val json = JSONObject().apply {
            put("type", channel.id)
        }
        val config = ChannelConfiguration(json)
        if (channel.customizable) {
            val activityClass = ChannelConfigActivities.channelTypeToActivity[channel.id]
            if (activityClass == null) {
                val snackBar = Snackbar.make(fragmentContainer, "Channel can be customized. Please update this app.", LENGTH_LONG)
                snackBar.setAction("Set Anyway", { sendConfig(config) })
                snackBar.show()
            } else {
                val intent = Intent(this, activityClass)
                intent.putExtra(ChannelConfigActivity.ARG_CONFIG, config)
                startActivityForResult(intent, CONFIG_ACTIVITY_REQUEST)
            }
        } else {
            sendConfig(config)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CONFIG_ACTIVITY_REQUEST) {
            when (resultCode) {
                ChannelConfigActivity.RESULT_CONFIG_SET -> {
                    val config = data?.getParcelableExtra<ChannelConfiguration>(ChannelConfigActivity.ARG_CONFIG)
                    if (config != null) {
                        sendConfig(config)
                    }
                }
            }
        }
    }

    private fun sendConfig(config: ChannelConfiguration) {
        val connection = App.instance.client.activeConnection!!
        connection.sendSetPlane(direction, config)

        App.instance.projector?.addObserver(projectorObserver)
    }

    private fun projectorDidUpdate() {
        finish()
    }
}
