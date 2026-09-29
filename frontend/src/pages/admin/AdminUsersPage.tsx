import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { adminApi, queryKeys } from '../../api/queries'
import { AdminTable, Pagination, TableSkeleton } from '../../components/admin/AdminTable'
import { Badge } from '../../components/ui/Badge'
import { ErrorState } from '../../components/ui/States'
import { errorMessage } from '../../lib/api'
import { formatDateTime } from '../../lib/format'

export function AdminUsersPage() {
  const [page, setPage] = useState(0)
  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: queryKeys.adminUsers(page),
    queryFn: () => adminApi.users(page),
    placeholderData: keepPreviousData,
  })

  if (isPending) return <TableSkeleton />
  if (isError) return <ErrorState message={errorMessage(error)} onRetry={() => refetch()} />

  return (
    <>
      <AdminTable caption="Users" head={['Name', 'Email', 'Role', 'Confirmed bookings', 'Joined']}>
        {data.content.map((user) => (
          <tr key={user.id}>
            <td className="px-4 py-3 font-medium">{user.name}</td>
            <td className="px-4 py-3 text-zinc-600">{user.email}</td>
            <td className="px-4 py-3">{user.role === 'ADMIN' ? <Badge tone="success">Admin</Badge> : <Badge>User</Badge>}</td>
            <td className="px-4 py-3 tabular-nums">{user.confirmedBookings}</td>
            <td className="px-4 py-3 whitespace-nowrap text-zinc-500">{formatDateTime(user.createdAt)}</td>
          </tr>
        ))}
      </AdminTable>
      <Pagination page={data} onChange={setPage} />
    </>
  )
}
