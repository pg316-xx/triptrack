import { Client } from '@stomp/stompjs'
import { API_URL, tokens } from './client'

// Backend: STOMP endpoint /ws-trip (plain WebSocket, no SockJS), JWT in the CONNECT
// "Authorization" header, broker prefix /topic, app prefix /app.
export function connectSocket(subscriptions) {
  const client = new Client({
    brokerURL: API_URL.replace(/^http/, 'ws') + '/ws-trip',
    connectHeaders: { Authorization: `Bearer ${tokens.access}` },
    reconnectDelay: 5000,
    onConnect: () => {
      subscriptions.forEach(({ topic, handler }) =>
        client.subscribe(topic, (msg) => handler(JSON.parse(msg.body))),
      )
    },
  })
  client.activate()
  return client
}

export const sendChat = (client, tripId, message) =>
  client.publish({
    destination: `/app/trip/${tripId}/chat`,
    body: JSON.stringify({ message }),
  })
